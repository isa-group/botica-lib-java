package es.us.isa.botica.bot;

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
 * <p><b>Usage example:</b>
 *
 * <pre>
 * public class MyBot extends BaseBot {
 *
 *   &#64;OrderHandler("analyze_data")
 *   public void analyzeData(String data) {
 *     System.out.println("Analyzing data: " + data);
 *     publishOrder("my message", "key", "order");
 *   }
 * }
 * </pre>
 *
 * <b>Note</b>: When using IoC frameworks like Spring or Guice, annotation-based configuration
 * might not be detected on proxied instances. For best results, instantiate bot classes manually or
 * use factory methods that return the actual implementation. Alternatively, order handlers can also
 * be registered programmatically via the functional approach:
 *
 * <pre>
 * registerOrderListener("analyze_data", message -> {
 *   System.out.println("Analyzing data: " + message);
 *   publishOrder("my message", "key", "order");
 * });
 * </pre>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface OrderHandler {
  /**
   * Specifies the orders this method listens to.
   *
   * @return the order names
   */
  String[] value();
}
