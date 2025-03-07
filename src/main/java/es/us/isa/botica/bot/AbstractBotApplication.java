package es.us.isa.botica.bot;

import es.us.isa.botica.bot.shutdown.ShutdownHandler;
import es.us.isa.botica.protocol.OrderListener;
import java.io.File;

/**
 * Helper class to define the logic of a bot.
 *
 * <p>Simply extend this class and configure the bot. Most basic usage:
 *
 * <ul>
 *   <li><b>For proactive bots:</b> define the bot action:
 *       <pre>
 * public MyBot extends AbstractBotApplication {
 *   private static Logger logger = Logger.getLogger(MyBot.class);
 *
 *   &#64;Override
 *   public void executeAction() {
 *     logger.info("Executing proactive action...");
 *     publishOrder("my message"); // order and key are taken from the lifecycle section of this bot's type in the main configuration file.
 *   }
 * }
 *   </pre>
 *   <li><b>For reactive bots:</b> define the action for a received order:
 *       <pre>
 * public MyBot extends AbstractBotApplication {
 *   private static Logger logger = Logger.getLogger(MyBot.class);
 *
 *   &#64;Override
 *   public void onOrderReceived(String message) {
 *     logger.info("Order received. Message: " + message);
 *     publishOrder("my message"); // order and key are taken from the lifecycle section of this bot's type in the main configuration file.
 *   }
 * }
 *   </pre>
 * </ul>
 *
 * Use {@link BotApplicationRunner#run(AbstractBotApplication, String[])} to run your bot
 * application.
 *
 * @author Alberto Mimbrero
 */
public abstract class AbstractBotApplication {
  private Bot bot;

  /**
   * Called before the bot is started and the connection with the message broker is established.
   *
   * <p>Override this method to perform additional configuration, such as registering shutdown hooks
   * or setting up extra order listeners.
   */
  public void configure() {}

  /**
   * Called when the bot starts running.
   *
   * <p>Override this method to define any startup logic, such as initializing resources or logging
   * bot startup details.
   */
  public void onStart() {}

  /**
   * Executes the bot's proactive action at regular intervals.
   *
   * <p>The execution schedule (initial delay and period) is defined in the bot's lifecycle
   * configuration.
   *
   * <p><b>Note:</b> This method is only invoked if the bot's lifecycle type is {@code proactive}.
   */
  public void executeAction() {}

  /**
   * Called when a message is received with the order that this bot's type listens to.
   *
   * <p>The order to listen is taken from the lifecycle section of this bot's type in the main
   * configuration file.
   *
   * <p><b>NOTE:</b> this method will not be called if the bot's lifecycle type is not {@code
   * reactive}.
   *
   * @param message the message received
   */
  public void onOrderReceived(String message) {}

  /**
   * Registers the given listener for the provided order.
   *
   * @param order the order to listen to
   * @param orderListener the listener to register
   */
  protected void registerOrderListener(String order, OrderListener orderListener) {
    this.bot.registerOrderListener(order, orderListener);
  }

  /** Returns the ID of this bot. */
  protected String getBotId() {
    return this.bot.getConfiguration().getId();
  }

  /** Returns the type of this bot. */
  protected String getBotType() {
    return this.bot.getTypeConfiguration().getId();
  }

  /**
   * Publishes an order with the given message. The key and order are taken from the main
   * configuration file.
   *
   * @param message the message of the order
   * @throws IllegalStateException if the bot type configuration does not specify a publish section
   */
  protected void publishOrder(String message) {
    this.bot.publishOrder(message);
  }

  /**
   * Publishes an order with the given key.
   *
   * @param key the key to publish the order with
   * @param order the order to publish
   * @param message the message of the order
   */
  protected void publishOrder(String key, String order, String message) {
    this.bot.publishOrder(key, order, message);
  }

  /** Returns the hostname of this bot's container. */
  protected String getHostname() {
    return this.bot.getHostname();
  }

  /**
   * Returns the hostname of the given bot's container.
   *
   * @param botId the ID of the bot instance
   * @return the bot container's hostname
   */
  protected String getBotHostname(String botId) {
    return this.bot.getBotHostname(botId);
  }

  /** Returns the shared directory for all bots provided by Botica. */
  protected File getSharedDirectory() {
    return this.bot.getSharedDirectory();
  }

  /** Returns the shutdown handler of this bot. */
  protected ShutdownHandler getShutdownHandler() {
    return this.bot.getShutdownHandler();
  }

  /** Returns the underlying {@link Bot} instance of this application. */
  protected Bot getBot() {
    return this.bot;
  }

  void setBot(Bot bot) {
    this.bot = bot;
  }
}
