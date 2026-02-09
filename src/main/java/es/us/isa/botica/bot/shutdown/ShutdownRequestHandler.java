package es.us.isa.botica.bot.shutdown;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a method as a shutdown request handler.
 *
 * <p>Methods annotated with {@code @ShutdownRequestHandler} will be automatically detected and
 * registered to handle shutdown requests from the Botica director.
 *
 * <h2>Usage examples:</h2>
 *
 * <p><b>Saving important data:</b>
 *
 * <pre>
 * public class MyBot extends BaseBot {
 *
 *   &#64;ShutdownRequestHandler
 *   public void onShutdownRequest(ShutdownRequest request) {
 *     this.saveData();
 *   }
 * }
 * </pre>
 *
 * <p><b>Canceling shutdown requests:</b>
 *
 * <pre>
 * public class MyBot extends BaseBot {
 *
 *   &#64;ShutdownRequestHandler
 *   public ShutdownResponse onShutdownRequest(ShutdownRequest request) {
 *     if (this.isRunning()) {
 *       return ShutdownResponse.cancel(); // This has no effect if request.isForced() is true.
 *     }
 *     return ShutdownResponse.ready();
 *   }
 * }
 * </pre>
 *
 * <p><b>Note</b>: If a shutdown request is forced, canceling it will have no effect.
 *
 * <p><b>Important:</b> Even if a shutdown request is <b>not forced</b>, canceling it is not
 * guaranteed to work. The director may still proceed with the shutdown based on other factors, such
 * as exceeding the allowed delay.
 *
 * <p><b>Note</b>: When using IoC frameworks like Spring or Guice, annotation-based configuration
 * might not be detected on proxied instances. For best results, instantiate bot classes manually or
 * use factory methods that return the actual implementation. Alternatively, shutdown request hooks
 * can also be registered programmatically via the functional approach:
 *
 * <pre>
 * getShutdownHandler().registerShutdownRequestHook((request, response) -> {
 *   if (this.isRunning()) {
 *     response.setCanceled(true); // This has no effect if request.isForced() is true.
 *   }
 * });
 * </pre>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface ShutdownRequestHandler {}
