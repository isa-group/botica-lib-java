package es.us.isa.botica.bot;

import es.us.isa.botica.BoticaConstants;
import es.us.isa.botica.bot.shutdown.ShutdownHandler;
import es.us.isa.botica.configuration.bot.BotInstanceConfiguration;
import es.us.isa.botica.configuration.bot.BotPublishConfiguration;
import es.us.isa.botica.configuration.bot.BotTypeConfiguration;
import es.us.isa.botica.configuration.bot.lifecycle.BotLifecycleConfiguration;
import es.us.isa.botica.configuration.bot.lifecycle.BotLifecycleType;
import es.us.isa.botica.configuration.bot.lifecycle.ProactiveBotLifecycleConfiguration;
import es.us.isa.botica.configuration.bot.lifecycle.ReactiveBotLifecycleConfiguration;
import es.us.isa.botica.protocol.BoticaClient;
import es.us.isa.botica.protocol.HeartbeatPacket;
import es.us.isa.botica.protocol.OrderListener;
import es.us.isa.botica.protocol.client.ReadyPacket;
import java.io.File;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A bot instance in a botica environment.
 *
 * <p>{@link #start() Starting} the bot does not block any thread. Necessary threads are started
 * internally for broker message listeners and other tasks.
 *
 * <p><b>NOTE:</b> it is recommended to extend the {@link BaseBot} helper class and use its
 * convenience methods in order to implement bots. For a more advanced use, the bot object can be
 * accessed with {@link BaseBot#getBot()}.
 *
 * @author Alberto Mimbrero
 */
public class Bot {
  private static final File SHARED_DIRECTORY = new File("/shared");

  private final Logger log;
  private final ShutdownHandler shutdownHandler;

  private final BotTypeConfiguration botTypeConfiguration;
  private final BotInstanceConfiguration botConfiguration;

  private final BoticaClient boticaClient;
  private boolean running = false;

  private Runnable proactiveTask;

  public Bot(
      BoticaClient boticaClient,
      BotTypeConfiguration botTypeConfiguration,
      BotInstanceConfiguration botConfiguration) {
    this.boticaClient = boticaClient;
    this.shutdownHandler = new ShutdownHandler(this.boticaClient);

    this.botTypeConfiguration = botTypeConfiguration;
    this.botConfiguration = botConfiguration;

    this.log = LoggerFactory.getLogger("Bot - " + botConfiguration.getId());
  }

  /**
   * Sets the proactive task for this bot.
   *
   * @param task the task to set
   * @throws IllegalStateException if the bot lifecycle type is not {@code proactive}
   */
  public void setProactiveTask(Runnable task) {
    if (this.getLifecycleConfiguration().getType() != BotLifecycleType.PROACTIVE) {
      throw new IllegalStateException(
          "Cannot register a proactive task because this bot is not configured as proactive.");
    }
    this.proactiveTask = task;
  }

  /** Returns whether the proactive task is set. */
  public boolean isProactiveTaskSet() {
    return this.proactiveTask != null;
  }

  /**
   * Registers the given listener for the order this bot serves. The order is taken from the main
   * configuration file.
   *
   * @param orderListener the listener to register
   * @throws IllegalStateException if no default order is specified for this bot in the current
   *     botica environment configuration
   */
  public void registerOrderListener(OrderListener orderListener) {
    BotLifecycleConfiguration lifecycleConfiguration = this.getLifecycleConfiguration();
    String order =
        lifecycleConfiguration instanceof ReactiveBotLifecycleConfiguration
            ? ((ReactiveBotLifecycleConfiguration) lifecycleConfiguration).getOrder()
            : null;

    if (order == null) {
      throw new IllegalStateException(
          "No default order specified for this bot in the environment configuration file");
    }
    this.registerOrderListener(order, orderListener);
  }

  /**
   * Registers the given listener for the provided order.
   *
   * @param order the order to listen to
   * @param orderListener the listener to register
   */
  public void registerOrderListener(String order, OrderListener orderListener) {
    this.boticaClient.registerOrderListener(Objects.requireNonNull(order), orderListener);
  }

  /**
   * Publishes an order with the given message. The key and order are taken from the main
   * configuration file.
   *
   * @param message the message of the order
   * @throws IllegalStateException if the bot type configuration does not specify a publish section
   */
  public void publishOrder(String message) {
    BotPublishConfiguration publishConfiguration =
        this.botTypeConfiguration.getPublishConfiguration();
    String key = publishConfiguration.getKey();
    String order = publishConfiguration.getOrder();
    if (key == null || key.isBlank() || order == null || order.isBlank()) {
      throw new IllegalStateException(
          "Cannot publish order: no publish section present in the bot type configuration.");
    }
    this.publishOrder(publishConfiguration.getKey(), publishConfiguration.getOrder(), message);
  }

  /**
   * Publishes an order with the given key.
   *
   * @param key the key to publish the order with
   * @param order the order to publish
   * @param message the message of the order
   */
  public void publishOrder(String key, String order, String message) {
    this.boticaClient.publishOrder(
        Objects.requireNonNull(key),
        Objects.requireNonNull(order),
        Objects.requireNonNull(message));
  }

  /** Returns the hostname of this bot's container. */
  public String getHostname() {
    return this.getBotHostname(this.botConfiguration.getId());
  }

  /**
   * Returns the hostname of the given bot's container.
   *
   * @param botId the ID of the bot instance
   * @return the bot container's hostname
   */
  public String getBotHostname(String botId) {
    return BoticaConstants.CONTAINER_PREFIX + botId;
  }

  /** Returns the shared directory for all bots provided by Botica. */
  public File getSharedDirectory() {
    return SHARED_DIRECTORY;
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

    if (this.getLifecycleConfiguration().getType() == BotLifecycleType.PROACTIVE) {
      this.startProactiveScheduler();
    }
    this.setupHeartbeat();
    this.boticaClient.sendPacket(new ReadyPacket());
    log.info("Bot started.");
  }

  private void setupHeartbeat() {
    this.boticaClient.registerPacketListener(
        HeartbeatPacket.class, packet -> this.boticaClient.sendPacket(new HeartbeatPacket()));
  }

  private void startProactiveScheduler() {
    if (this.proactiveTask == null) {
      throw new IllegalStateException(
          "This bot is configured as a proactive bot, but no proactive task has been registered.");
    }
    ProactiveBotLifecycleConfiguration lifecycleConfiguration =
        (ProactiveBotLifecycleConfiguration) this.getLifecycleConfiguration();

    ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();
    if (lifecycleConfiguration.getPeriod() > 0) {
      this.scheduleRepeatingTask(lifecycleConfiguration, executorService);
    } else {
      this.scheduleTask(executorService, lifecycleConfiguration);
    }
  }

  private void scheduleTask(
      ScheduledExecutorService executorService,
      ProactiveBotLifecycleConfiguration lifecycleConfiguration) {
    executorService.schedule(
        () -> {
          if (isRunning()) {
            this.runProactiveTask();
            this.stop();
          }
          executorService.shutdown();
        },
        lifecycleConfiguration.getInitialDelay(),
        TimeUnit.SECONDS);
  }

  private void scheduleRepeatingTask(
      ProactiveBotLifecycleConfiguration lifecycleConfiguration,
      ScheduledExecutorService executorService) {
    executorService.scheduleWithFixedDelay(
        () -> {
          if (!isRunning()) {
            executorService.shutdownNow();
            return;
          }
          this.runProactiveTask();
        },
        lifecycleConfiguration.getInitialDelay(),
        lifecycleConfiguration.getPeriod(),
        TimeUnit.SECONDS);
  }

  private void runProactiveTask() {
    try {
      this.proactiveTask.run();
    } catch (Exception e) {
      log.error("an exception was risen during the bot proactive task", e);
    }
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
    log.info("Closing connection with the message broker...");
    this.boticaClient.close();
    this.running = false;
    log.info("Bot stopped.");
  }

  private BotLifecycleConfiguration getLifecycleConfiguration() {
    return botConfiguration.getLifecycleConfiguration() != null
        ? botConfiguration.getLifecycleConfiguration()
        : botTypeConfiguration.getLifecycleConfiguration();
  }
}
