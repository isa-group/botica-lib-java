# Handling shutdown requests from the Director

Your Botica bot can gracefully respond to shutdown requests initiated by the Botica Director. Proper
shutdown handling allows your bot to perform critical cleanup, save its state, or even attempt to
delay its termination if necessary.

## Understanding shutdown requests

The Botica Director manages the lifecycle of all bots, including their shutdown. When the Director
decides to terminate a bot container, it sends a shutdown request. This request can be either:

- **Graceful**: The Director requests the bot to shut down, allowing it a limited time to respond
  and complete any critical tasks. The bot can potentially *cancel* this type of shutdown if it's
  currently busy.
- **Forced**: The Director mandates immediate termination. In this scenario, any attempt to cancel
  the shutdown will be ignored, and the bot should prioritize quick, essential cleanup.

## Annotation-based shutdown handler

You can define a method in your `BaseBot` implementation and mark it with the
`@ShutdownRequestHandler` annotation to automatically handle incoming shutdown requests.

### Simple cleanup

A basic `@ShutdownRequestHandler` method can be defined with no parameters and a `void` return type.
This is suitable for straightforward cleanup tasks that don't need information about the request or
to influence the shutdown decision.

```java
import es.us.isa.botica.bot.shutdown.ShutdownRequestHandler;

public class CleanupBot extends BaseBot {

  @ShutdownRequestHandler
  public void onShutdown() {
    System.out.println("Bot is receiving a shutdown request. Performing final cleanup...");
    // Perform cleanup like saving ephemeral state or closing local resources.
    clearTemporaryFiles();
  }

  private void clearTemporaryFiles() { /* ... */ }
}
```

### Accessing the request and controlling the response

Your `@ShutdownRequestHandler` method can also accept a `ShutdownRequest` object as a parameter
and/or return a `ShutdownResponse` object.

When you define the method to accept a `ShutdownRequest` parameter, Botica will inject an object
containing details about the shutdown (e.g., if it's forced). This allows you to perform conditional
cleanup or logging based on the nature of the request.

```java
import es.us.isa.botica.bot.shutdown.ShutdownRequest;
import es.us.isa.botica.bot.shutdown.ShutdownRequestHandler;

public class ConditionalCleanupBot extends BaseBot {

  @ShutdownRequestHandler
  public void onShutdown(ShutdownRequest request) {
    if (request.isForced()) {
      System.out.println("Forced shutdown detected. Saving critical data quickly.");
      saveCriticalData();
    } else {
      System.out.println("Normal shutdown. Cleaning up everything.");
      performFullCleanup();
    }
  }

  private void saveCriticalData() { /* ... */ }

  private void performFullCleanup() { /* ... */ }
}
```

By returning a `ShutdownResponse` object, your bot can signal its readiness to shut down or attempt
to cancel the request. If your bot is currently executing a critical task and the shutdown is *not
forced*, returning `ShutdownResponse.cancel()` can request the Director to delay the shutdown.

```java
import es.us.isa.botica.bot.shutdown.ShutdownRequest;
import es.us.isa.botica.bot.shutdown.ShutdownResponse;
import es.us.isa.botica.bot.shutdown.ShutdownRequestHandler;

public class InterruptibleBot extends BaseBot {

  private volatile boolean isBusy = true; // Assume this state changes based on bot's work

  @ShutdownRequestHandler
  public ShutdownResponse onShutdown(ShutdownRequest request) {
    if (request.isForced() || !isBusy) {
      System.out.println("Shutting down gracefully.");
      saveAllState();
      return ShutdownResponse.ready(); // Signals readiness for shutdown
    }
    System.out.println("Still busy, attempting to cancel shutdown.");
    return ShutdownResponse.cancel(); // Requests to cancel the shutdown. May be ignored if forced.
  }

  private void saveAllState() { /* ... */ }
}
```

> [!NOTE]
> When using IoC frameworks like Spring or Guice, annotation-based configuration might not be
> detected on proxied instances. For best results, instantiate bot classes manually or use factory
> methods that return the actual implementation. Alternatively, shutdown request hooks can also be
> registered programmatically, as shown below.

## Programmatic shutdown handling

You can also register a `ShutdownRequestHook` programmatically within your `configure()` method.
This provides the same functionality as the annotation-based approach but offers more flexibility
for dynamic registration.

To do this, use `getShutdownHandler().registerShutdownRequestHook(ShutdownRequestHook hook)`.

```java
import es.us.isa.botica.bot.BaseBot;
import es.us.isa.botica.bot.shutdown.ShutdownRequest;
import es.us.isa.botica.bot.shutdown.ShutdownResponse;

public class ProgrammaticShutdownBot extends BaseBot {

  private volatile boolean isBusy = true; // Assume this state changes based on bot's work

  @Override
  public void configure() {
    getShutdownHandler().registerShutdownRequestHook((request, response) -> {
      if (request.isForced()) {
        System.out.println("Forced shutdown hook. Performing quick save.");
        quickSave();
      } else if (isBusy) {
        System.out.println("Still busy, attempting to cancel shutdown.");
        response.setCanceled(true); // Attempt to cancel the shutdown
      } else {
        System.out.println("Not busy, ready for shutdown.");
      }
    });
  }

  private void quickSave() { /* ... */ }
}
```

## Important considerations

- **Forced Shutdowns**: When `ShutdownRequest.isForced()` is `true`, any attempt to cancel the
  shutdown via `ShutdownResponse.cancel()` will be ignored. Your bot should prioritize immediate,
  critical data saving in this scenario.
- **Timeliness**: Even if a shutdown is not forced, the Director has a timeout for bot responses.
  Ensure your cleanup logic is efficient to avoid the Director forcibly terminating your container
  if it takes too long to respond.
- **Multiple Hooks**: If multiple `ShutdownRequestHandler` methods or `ShutdownRequestHook`s are
  registered, they will all be executed. If any hook requests cancellation (
  `response.setCanceled(true)`), the overall response will be a cancellation attempt (unless the
  shutdown is forced).

[Back to documentation index](0-index.md)
