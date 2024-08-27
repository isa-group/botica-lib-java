package es.us.isa.botica.bot;

import es.us.isa.botica.client.OrderListener;

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
   * <p>Use this method to configure the bot beyond the proactive action or the main order listener,
   * like registering a shutdown hook.
   */
  public void configure() {}

  /**
   * Called repeatedly to execute the proactive action of the bot.
   *
   * <p>The initial delay and period parameters are taken from the lifecycle section of this bot's
   * type in the main configuration file.
   *
   * <p><b>NOTE:</b> this method will not be called if the bot's lifecycle type is not {@code
   * proactive}.
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

  /** Returns the underlying {@link Bot} instance of this application. */
  protected Bot getBot() {
    return this.bot;
  }

  void setBot(Bot bot) {
    this.bot = bot;
  }
}
