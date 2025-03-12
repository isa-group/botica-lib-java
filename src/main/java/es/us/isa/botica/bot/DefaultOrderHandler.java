package es.us.isa.botica.bot;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a method as the default order handler for a bot.
 *
 * <p>Methods annotated with {@code @DefaultOrderHandler} will be automatically registered to handle
 * the default order defined in the bot's lifecycle configuration.
 *
 * <p><b>Note:</b> There should be only one method annotated with {@code @DefaultOrderHandler} per
 * bot. If multiple methods are annotated, an exception will be thrown.
 *
 * <p><b>Usage example:</b>
 *
 * <pre>
 * public class MyBot extends BaseBot {
 *
 *   &#64;DefaultOrderHandler
 *   public void handleOrder(String message) {
 *     System.out.println("Analyzing data: " + message);
 *     publishOrder("my message", "key", "order");
 *   }
 * }
 * </pre>
 *
 * <b>Note</b>: When using IoC frameworks like Spring or Guice, annotation-based configuration might
 * not be detected on proxied instances. For best results, instantiate bot classes manually or use
 * factory methods that return the actual implementation. Alternatively, default order handlers can
 * also be registered programmatically via the functional approach:
 *
 * <pre>
 * registerOrderListener(message -> {
 *   System.out.println("Analyzing data: " + message);
 *   publishOrder("my message", "key", "order");
 * });
 * </pre>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface DefaultOrderHandler {}
