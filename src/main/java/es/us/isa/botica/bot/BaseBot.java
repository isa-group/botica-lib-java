package es.us.isa.botica.bot;

import es.us.isa.botica.bot.order.OrderMessageTypeConverter;
import es.us.isa.botica.bot.shutdown.ShutdownHandler;
import es.us.isa.botica.inspect.ComponentInspector;
import es.us.isa.botica.protocol.OrderListener;
import java.io.File;
import java.lang.reflect.Type;

/**
 * Base class for defining a Botica bot.
 *
 * <p>This class provides a structured way to create a bot, whether proactive or reactive.
 * Developers should extend {@code BaseBot} and define their bot's behavior using the provided
 * lifecycle methods and annotations.
 *
 * <p>Basic usage:
 *
 * <ul>
 *   <li><b>For proactive bots:</b> Define a periodic task using {@link
 *       ProactiveTask @ProactiveTask}:
 *       <pre>
 * public class MyBot extends BaseBot {
 *
 *   &#64;ProactiveTask
 *   public void executeProactiveTask() {
 *     System.out.println("Executing proactive task...");
 *     publishOrder("my message", "key", "order");
 *   }
 * }
 *       </pre>
 *   <li><b>For reactive bots:</b> Define order handlers using {@link OrderHandler @OrderHandler}:
 *       <pre>
 * public class MyBot extends BaseBot {
 *
 *   &#64;OrderHandler("analyze_data")
 *   public void analyzeData(String data) {
 *     System.out.println("Analyzing data: " + data);
 *     publishOrder("my results", "key", "order");
 *   }
 * }
 *       </pre>
 * </ul>
 *
 * <p>Use {@link BotLauncher#run(BaseBot)} to start your bot.
 *
 * @author Alberto Mimbrero
 */
public abstract class BaseBot {
  private final ComponentInspector componentInspector = new ComponentInspector();
  private Bot bot;

  /**
   * Called before the bot starts and the connection with the message broker is established.
   *
   * <p>Override this method to perform additional configuration, such as:
   *
   * <ul>
   *   <li>Registering shutdown hooks
   *   <li>Setting up additional order listeners programmatically
   *   <li>Registering {@link OrderMessageTypeConverter order message type converters}
   * </ul>
   *
   * <p><b>Note:</b> This method is intended only for bot configuration. The connection with the
   * message broker is <b>not yet established</b> at this stage, so attempting to send messages will
   * result in an error. However, you can safely register order listeners here.
   */
  public void configure() {}

  /**
   * Called after the bot has started and the connection with the message broker is established.
   *
   * <p>Override this method to define any startup logic, such as sending initial messages or
   * requests to other bots.
   *
   * <p><b>Note:</b> Unlike {@link #configure()}, this method is called <b>after</b> the bot is
   * fully connected to the message broker. You can safely send messages and interact with other
   * bots at this point.
   */
  public void onStart() {}

  /**
   * Sets the proactive task for this bot.
   *
   * @param task the task to set
   * @throws IllegalStateException if the bot lifecycle type is not {@code proactive}
   */
  public void setProactiveTask(Runnable task) {
    this.bot.setProactiveTask(task);
  }

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
    return this.bot.getConfiguration().getTypeConfiguration().getId();
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

  public ComponentInspector getComponentInspector() {
    return componentInspector;
  }

  /**
   * Registers a type converter for handling parameters in {@code @OrderHandler} methods.
   *
   * <p>This converter will transform incoming message strings into objects of the types provided in
   * the given converter when a handler method expects a parameter of the registered type.
   *
   * <p><strong>IMPORTANT:</strong> Converters must be registered during the {@link #configure()}
   * phase of the bot's lifecycle. Registrations made later may have no effect.
   *
   * @param converter the converter implementation for transforming messages of the specified types
   *     in {@link OrderMessageTypeConverter#getSupportedTypes()}
   * @see OrderMessageTypeConverter
   */
  protected void registerOrderMessageTypeConverter(OrderMessageTypeConverter<?> converter) {
    this.componentInspector.registerOrderMessageTypeConverter(converter);
  }

  /**
   * Registers a type converter for handling parameters in {@code @OrderHandler} methods.
   *
   * <p>This converter will transform incoming message strings into objects of type {@code T} when a
   * handler method expects a parameter of the registered type.
   *
   * <p><strong>IMPORTANT:</strong> Converters must be registered during the {@link #configure()}
   * phase of the bot's lifecycle. Registrations made later may have no effect.
   *
   * @param <T> the target type that the converter produces
   * @param type the class representing the parameter type this converter handles
   * @param converter the converter implementation for transforming messages to type T
   * @see OrderMessageTypeConverter
   */
  protected <T> void registerOrderMessageTypeConverter(
      Class<T> type, OrderMessageTypeConverter<T> converter) {
    this.componentInspector.registerOrderMessageTypeConverter(type, converter);
  }

  /**
   * Registers a type converter for handling parameters in {@code @OrderHandler} methods.
   *
   * <p>This converter will transform incoming message strings into objects of the given type when a
   * handler method expects a parameter of the registered type.
   *
   * <p><strong>IMPORTANT:</strong> Converters must be registered during the {@link #configure()}
   * phase of the bot's lifecycle. Registrations made later may have no effect.
   *
   * @param type the class representing the parameter type this converter handles
   * @param converter the converter implementation for transforming messages to the given type
   * @see OrderMessageTypeConverter
   */
  protected void registerOrderMessageTypeConverter(
      Type type, OrderMessageTypeConverter<?> converter) {
    this.componentInspector.registerOrderMessageTypeConverter(type, converter);
  }

  /** Returns the underlying {@link Bot} instance of this object. */
  protected Bot getBot() {
    return this.bot;
  }

  void setBot(Bot bot) {
    this.bot = bot;
  }
}
