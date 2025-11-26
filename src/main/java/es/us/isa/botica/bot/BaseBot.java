package es.us.isa.botica.bot;

import es.us.isa.botica.bot.payload.PayloadDeserializer;
import es.us.isa.botica.bot.payload.PayloadSerializer;
import es.us.isa.botica.bot.payload.support.JacksonPayloadSerializer;
import es.us.isa.botica.bot.shutdown.ShutdownHandler;
import es.us.isa.botica.bot.shutdown.ShutdownRequestHook;
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
 *     publishOrder("key", "action", "payload");
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
 *     publishOrder("key", "action", "my results");
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
   *   <li>Registering {@link ShutdownRequestHook shutdown hooks}
   *   <li>Setting up additional order listeners {@link #registerOrderListener(String,
   *       OrderListener) programmatically}
   *   <li>Registering {@link #registerPayloadSerializer(PayloadSerializer) payload serializers} and
   *       {@link #registerPayloadDeserializer(PayloadDeserializer) deserializers}
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
   * Registers the given listener for the default action of this bot (defined in the botica
   * environment file).
   *
   * @param orderListener the listener to register
   * @throws IllegalStateException if no default action is specified for this bot in the current
   *     botica environment configuration
   */
  public void registerDefaultOrderListener(OrderListener orderListener) {
    this.bot.registerDefaultOrderListener(orderListener);
  }

  /**
   * Registers the given order listener for the provided action.
   *
   * @param action the action to listen to
   * @param orderListener the listener to register
   */
  protected void registerOrderListener(String action, OrderListener orderListener) {
    this.bot.registerOrderListener(action, orderListener);
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
   * Publishes an order with the given payload. The key and action are taken from this bot's publish
   * defaults section in the environment file.
   *
   * <p>The provided {@code payload} object is automatically serialized into a {@code String} before
   * being published. See {@link #publishOrder(String, String, Object)} for details on payload
   * serialization.
   *
   * @param payload the payload of the order
   * @throws IllegalStateException if the bot type configuration does not specify a publish section
   */
  protected void publishDefaultOrder(Object payload) {
    this.bot.publishDefaultOrder(payload);
  }

  /**
   * Publishes an order with the given key and action.
   *
   * <p>The provided {@code payload} object is automatically serialized into a {@code String} before
   * being published.
   *
   * <p>By default, the system supports the following payload types:
   *
   * <ul>
   *   <li>{@code String} payloads (passed directly without modification).
   *   <li>{@code org.json.JSONObject} payloads (converted to their JSON string representation).
   *   <li>Any other Java type will be automatically serialized to a JSON string using an internal
   *       {@link JacksonPayloadSerializer}.
   * </ul>
   *
   * <p>Custom serialization logic for specific types can be provided by registering your own
   * implementations of {@link PayloadSerializer}. This allows you to control how various object
   * types are converted to strings for publishing.
   *
   * <p><b>Usage example with a custom POJO:</b>
   *
   * <pre>
   * public class MyBot extends BaseBot {
   *   static class MyCustomData {
   *     public String value;
   *     public int count;
   *
   *     public MyCustomData(String value, int count) {
   *       this.value = value;
   *       this.count = count;
   *     }
   *   }
   *
   *   &#64;ProactiveTask
   *   public void executeProactiveTask() {
   *     MyCustomData data = new MyCustomData("hello", 123);
   *     publishOrder("myKey", "myAction", data);
   *     // This will automatically serialize 'data' to {"value":"hello","count":123}
   *   }
   * }
   * </pre>
   *
   * @param key the key to publish the order with
   * @param action the action to publish
   * @param payload the payload of the order
   */
  protected void publishOrder(String key, String action, Object payload) {
    this.bot.publishOrder(key, action, payload);
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
   * Registers a payload deserializer for handling parameters in {@code @OrderHandler} methods.
   *
   * <p><strong>IMPORTANT:</strong> Deserializers must be registered during the {@link #configure()}
   * phase of the bot's lifecycle. Registrations made later may have no effect.
   *
   * @param deserializer the deserializer implementation
   * @see PayloadDeserializer
   */
  protected void registerPayloadDeserializer(PayloadDeserializer<?> deserializer) {
    this.componentInspector.registerPayloadDeserializer(deserializer);
  }

  /**
   * Registers a payload deserializer for handling parameters in {@code @OrderHandler} methods.
   *
   * <p><strong>IMPORTANT:</strong> Deserializers must be registered during the {@link #configure()}
   * phase of the bot's lifecycle. Registrations made later may have no effect.
   *
   * @param <T> the target type that the deserializer produces
   * @param type the class representing the parameter type this deserializer handles
   * @param deserializer the deserializer implementation
   * @see PayloadDeserializer
   */
  protected <T> void registerPayloadDeserializer(
      Class<T> type, PayloadDeserializer<T> deserializer) {
    this.componentInspector.registerPayloadDeserializer(type, deserializer);
  }

  /**
   * Registers a payload deserializer for handling parameters in {@code @OrderHandler} methods.
   *
   * <p><strong>IMPORTANT:</strong> Deserializers must be registered during the {@link #configure()}
   * phase of the bot's lifecycle. Registrations made later may have no effect.
   *
   * @param type the type representing the parameter type this deserializer handles
   * @param deserializer the deserializer implementation
   * @see PayloadDeserializer
   */
  protected void registerPayloadDeserializer(Type type, PayloadDeserializer<?> deserializer) {
    this.componentInspector.registerPayloadDeserializer(type, deserializer);
  }

  /**
   * Registers a payload serializer for handling object serialization in {@code publishOrder}
   * methods.
   *
   * <p><strong>IMPORTANT:</strong> Serializers must be registered during the {@link #configure()}
   * phase of the bot's lifecycle. Registrations made later may have no effect.
   *
   * @param serializer the serializer implementation
   * @see PayloadSerializer
   */
  protected void registerPayloadSerializer(PayloadSerializer<?> serializer) {
    this.bot.registerPayloadSerializer(serializer);
  }

  /**
   * Registers a payload serializer for handling object serialization in {@code publishOrder}
   * methods.
   *
   * <p><strong>IMPORTANT:</strong> Serializers must be registered during the {@link #configure()}
   * phase of the bot's lifecycle. Registrations made later may have no effect.
   *
   * @param <T> the source type that the serializer handles
   * @param type the class representing the object type this serializer handles
   * @param serializer the serializer implementation
   * @see PayloadSerializer
   */
  protected <T> void registerPayloadSerializer(Class<T> type, PayloadSerializer<T> serializer) {
    this.bot.registerPayloadSerializer(type, serializer);
  }

  /**
   * Registers a payload serializer for handling object serialization in {@code publishOrder}
   * methods.
   *
   * <p><strong>IMPORTANT:</strong> Serializers must be registered during the {@link #configure()}
   * phase of the bot's lifecycle. Registrations made later may have no effect.
   *
   * @param type the type representing the object type this serializer handles
   * @param serializer the serializer implementation
   * @see PayloadSerializer
   */
  protected void registerPayloadSerializer(Type type, PayloadSerializer<?> serializer) {
    this.bot.registerPayloadSerializer(type, serializer);
  }

  /** Returns the underlying {@link Bot} instance of this object. */
  protected Bot getBot() {
    return this.bot;
  }

  void setBot(Bot bot) {
    this.bot = bot;
  }
}
