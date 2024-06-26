package es.us.isa.botica.bot;

import es.us.isa.botica.client.BoticaClient;
import es.us.isa.botica.client.OrderListener;
import es.us.isa.botica.configuration.bot.BotInstanceConfiguration;
import es.us.isa.botica.configuration.bot.BotPublishConfiguration;
import es.us.isa.botica.configuration.bot.BotTypeConfiguration;
import es.us.isa.botica.configuration.bot.lifecycle.BotLifecycleConfiguration;
import es.us.isa.botica.configuration.bot.lifecycle.BotLifecycleType;
import es.us.isa.botica.configuration.bot.lifecycle.ProactiveBotLifecycleConfiguration;
import es.us.isa.botica.configuration.bot.lifecycle.ReactiveBotLifecycleConfiguration;
import es.us.isa.botica.support.ShutdownHandler;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A bot instance in a botica environment.
 *
 * <p>{@link #start() Starting} the bot does not block any thread. Necessary threads are started
 * internally for broker message listeners and proactive actions.
 *
 * <p><b>NOTE:</b> programmers are recommended to extend the {@link AbstractBotApplication} helper
 * class and use its convenience methods in order to create their own bots. For a more advanced use,
 * the bot object can be accessed with {@link AbstractBotApplication#getBot()}.
 *
 * @author Alberto Mimbrero
 */
public class Bot {
  private final Logger log;

  private final BotTypeConfiguration botTypeConfiguration;
  private final BotInstanceConfiguration botConfiguration;

  private final BoticaClient boticaClient;
  private boolean running = false;

  private Runnable proactiveAction;
  private ShutdownHandler shutdownHandler;

  public Bot(
      BoticaClient boticaClient,
      BotTypeConfiguration botTypeConfiguration,
      BotInstanceConfiguration botConfiguration) {
    this.boticaClient = boticaClient;
    this.botTypeConfiguration = botTypeConfiguration;
    this.botConfiguration = botConfiguration;

    this.log = LoggerFactory.getLogger("Bot - " + botConfiguration.getId());
  }

  /**
   * Sets the action for this bot.
   *
   * @param action the action to set
   * @throws IllegalStateException if the bot lifecycle type is not {@code proactive}
   */
  public void setProactiveAction(Runnable action) {
    if (this.getLifecycleConfiguration().getType() != BotLifecycleType.PROACTIVE) {
      throw new IllegalStateException("bot lifecycle type is not proactive");
    }
    this.proactiveAction = action;
  }

  /** Returns whether the proactive action is set. */
  public boolean isProactiveActionSet() {
    return this.proactiveAction != null;
  }

  /**
   * Registers the given listener for the order this bot serves. The order is taken from the main
   * configuration file.
   *
   * @param orderListener the listener to register
   * @throws IllegalStateException if the bot lifecycle type is not {@code reactive}
   */
  public void registerOrderListener(OrderListener orderListener) {
    if (this.getLifecycleConfiguration().getType() != BotLifecycleType.REACTIVE) {
      throw new IllegalStateException("bot lifecycle type is not reactive");
    }
    ReactiveBotLifecycleConfiguration lifecycleConfiguration =
        (ReactiveBotLifecycleConfiguration) this.getLifecycleConfiguration();
    this.registerOrderListener(lifecycleConfiguration.getOrder(), orderListener);
  }

  /**
   * Registers the given listener for the provided order.
   *
   * @param order the order to listen to
   * @param orderListener the listener to register
   * @throws IllegalStateException if the bot lifecycle type is not {@code reactive}
   */
  public void registerOrderListener(String order, OrderListener orderListener) {
    if (this.getLifecycleConfiguration().getType() != BotLifecycleType.REACTIVE) {
      throw new IllegalStateException("bot lifecycle type is not reactive");
    }
    this.boticaClient.registerOrderListener(order, orderListener);
  }

  /**
   * Publishes an order with the given message. The key and order are taken from the main
   * configuration file.
   *
   * @param message the message of the order
   */
  public void publishOrder(String message) {
    BotPublishConfiguration publishConfiguration =
        this.botTypeConfiguration.getPublishConfiguration();

    this.boticaClient.publishOrder(
        publishConfiguration.getKey(), publishConfiguration.getOrder(), message);
  }

  /**
   * Publishes an order with the given key.
   *
   * @param key the key to publish the order with
   * @param order the order to publish
   * @param message the message of the order
   */
  public void publishOrder(String key, String order, String message) {
    this.boticaClient.publishOrder(key, order, message);
  }

  /** Returns the shutdown handler of this bot */
  public ShutdownHandler getShutdownHandler() {
    return shutdownHandler;
  }

  /** Returns the configuration of this bot's type */
  public BotTypeConfiguration getTypeConfiguration() {
    return botTypeConfiguration;
  }

  /** Returns the configuration of this bot instance */
  public BotInstanceConfiguration getConfiguration() {
    return botConfiguration;
  }

  /**
   * Starts the bot.
   *
   * @throws TimeoutException if the connection with the message broker cannot be established
   */
  public void start() throws TimeoutException {
    log.info("Establishing connection with the message broker...");
    this.boticaClient.connect();
    this.running = true;
    log.info("Connected to the message broker.");

    this.shutdownHandler = new ShutdownHandler(this.boticaClient);

    if (this.getLifecycleConfiguration().getType() == BotLifecycleType.PROACTIVE) {
      this.startProactiveScheduler();
    }
    log.info("Bot started.");
  }

  private void startProactiveScheduler() {
    if (this.proactiveAction == null) {
      throw new IllegalStateException("undefined action for proactive bot");
    }

    ProactiveBotLifecycleConfiguration lifecycleConfiguration =
        (ProactiveBotLifecycleConfiguration) this.getLifecycleConfiguration();
    Timer timer = new Timer();
    timer.schedule(
        new TimerTask() {
          @Override
          public void run() {
            if (!isRunning()) {
              cancel();
              return;
            }
            try {
              proactiveAction.run();
            } catch (Exception e) {
              log.error("an exception was risen during the bot action", e);
            }
          }
        },
        lifecycleConfiguration.getInitialDelay() * 1000,
        lifecycleConfiguration.getPeriod() * 1000);
  }

  /** Returns whether the bot is running. */
  public boolean isRunning() {
    return this.running;
  }

  /** Closes the connection and stops the bot. */
  public void stop() {
    if (!this.running) {
      throw new IllegalStateException("bot is not running");
    }
    this.boticaClient.close();
  }

  private BotLifecycleConfiguration getLifecycleConfiguration() {
    return botConfiguration.getLifecycleConfiguration() != null
        ? botConfiguration.getLifecycleConfiguration()
        : botTypeConfiguration.getLifecycleConfiguration();
  }
}
