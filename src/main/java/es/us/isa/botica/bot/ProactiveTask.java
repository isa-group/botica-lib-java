package es.us.isa.botica.bot;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a method as the proactive task for a Botica bot.
 *
 * <p>Methods annotated with {@code @ProactiveTask} will be executed periodically, following the
 * delay and period settings in the bot's configuration.
 *
 * <p><b>Note:</b> This method will only be called if the bot's lifecycle type is {@code proactive}.
 *
 * <p><b>Usage example:</b>
 *
 * <pre>
 * public class MyBot extends BaseBot {
 *
 *   &#64;ProactiveTask
 *   public void performTask() {
 *     System.out.println("Executing proactive task...");
 *     publishOrder("key", "action", "my payload");
 *   }
 * }
 * </pre>
 *
 * <b>Note</b>: When using IoC frameworks like Spring or Guice, annotation-based configuration might
 * not be detected on proxied instances. For best results, instantiate bot classes manually or use
 * factory methods that return the actual implementation. Alternatively, proactive tasks can also be
 * registered programmatically via the functional approach:
 *
 * <pre>
 * setProactiveTask(() -> {
 *   publishOrder("key", "action", "my payload");
 * });
 * </pre>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface ProactiveTask {}
