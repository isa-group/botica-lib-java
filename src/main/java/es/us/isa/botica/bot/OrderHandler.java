package es.us.isa.botica.bot;

import es.us.isa.botica.bot.payload.PayloadDeserializer;
import es.us.isa.botica.bot.payload.support.JacksonPayloadDeserializer;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a method as an order handler.
 *
 * <p>Methods annotated with {@code @OrderHandler} will be automatically detected and registered to
 * process incoming orders.
 *
 * <p>Handler methods can have zero or one parameters. When a parameter is present, the system will
 * automatically convert the incoming message string to the parameter type using registered {@link
 * PayloadDeserializer}s. By default, the system supports:
 *
 * <ul>
 *   <li>{@code String} parameters (passes the message string directly)
 *   <li>{@code JSONMessage} parameters (parses the message as JSON)
 * </ul>
 *
 * <p>For automatic deserialization to any Java type, you can register the {@link
 * JacksonPayloadDeserializer Jackson deserializer}:
 *
 * <pre>
 * &#64;Override
 * public void configure() {
 *   registerPayloadDeserializer(new JacksonPayloadDeserializer());
 * }
 * </pre>
 *
 * <p>Custom deserializers can be registered using {@code registerPayloadDeserializer()}.
 *
 * <p><b>Usage example:</b>
 *
 * <pre>
 * public class MyBot extends BaseBot {
 *
 *   &#64;OrderHandler("analyze_data")
 *   public void analyzeData(String data) {
 *     System.out.println("Analyzing data: " + data);
 *     publishOrder("key", "action", "my payload");
 *   }
 * }
 * </pre>
 *
 * <b>Note</b>: When using IoC frameworks like Spring or Guice, annotation-based configuration might
 * not be detected on proxied instances. For best results, instantiate bot classes manually or use
 * factory methods that return the actual implementation. Alternatively, order handlers can also be
 * registered programmatically via the functional approach:
 *
 * <pre>
 * registerOrderListener("analyze_data", payload -> {
 *   System.out.println("Analyzing data: " + payload);
 *   publishOrder("key", "action", "my payload");
 * });
 * </pre>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface OrderHandler {
  /**
   * Specifies the actions this method listens to.
   *
   * @return the action names
   */
  String[] value();
}
