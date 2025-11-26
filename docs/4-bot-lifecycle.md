# Bot lifecycle

A Botica bot follows a straightforward lifecycle after being launched via
`BotLauncher.run(BaseBot botInstance)`. This process ensures the bot is properly configured,
connected, and ready to perform its tasks.

The general flow is as follows:

1. **Launch**: Your bot's main entry point calls `BotLauncher.run()`.
2. **Configuration Loading**: The bot loads its runtime configuration (ID, type, broker settings,
   etc.) from the environment, provided by the Botica Director.
3. **`configure()` method execution**: The `configure()` method of your `BaseBot` implementation is
   invoked, offering the first opportunity to set up custom behaviors.
4. **Component Scanning**: The bot's class is scanned for Botica-specific annotations (e.g.,
   `@OrderHandler`, `@ProactiveTask`, `@ShutdownRequestHandler`). Methods marked with these
   annotations are automatically registered.
5. **Broker Connection**: The bot establishes a connection to the message broker using the loaded
   configuration.
6. **Director Communication**: The bot communicates with the Botica Director, signaling its
   readiness and setting up necessary communication channels.
7. **`onStart()` method execution**: The `onStart()` method of your `BaseBot` implementation is
   invoked. At this point, the bot is fully connected and operational.

## Lifecycle events

`BaseBot` provides two lifecycle methods that you can override to hook into specific stages of the
bot's initialization.

### Configure

The `configure()` method is called early in the bot's lifecycle, after its core configuration is
loaded but **before** it attempts to connect to the message broker.

This phase is crucial for initial setup that doesn't rely on network connectivity. Here, you can
programmatically register order listeners, proactive tasks, or shutdown request hooks. You can also
register custom `PayloadSerializer`s and `PayloadDeserializer`s to extend the bot's ability to
handle different message types. <!-- TODO -->

> [!NOTE]
> The connection to the message broker is not yet established at this stage. Any attempts to publish
> or subscribe to orders will result in an error.

```java
@Override
public void configure() {
  System.out.println("Bot is configuring...");
  // Register a programmatic order listener
  registerOrderListener("setup_action", (action, payload) -> {
    System.out.println("Received setup action during configure: " + action);
  });
  // You might register custom payload deserializers here
  registerPayloadDeserializer(new MyCustomTypeDeserializer());
}
```

### On start

The `onStart()` method is invoked after the bot has successfully connected to the message broker and
all annotation-based and programmatic handlers have been registered.

This is the ideal place for any startup logic that requires the bot to be fully operational and
connected to the Botica environment. For instance, you might send initial orders to other bots or
services to kick-start processes, or perform actions that rely on full connectivity, such as
fetching initial data.

```java
@Override
public void onStart() {
  System.out.println("Bot has started and is connected.");
  // Publish an initial order to request data
  publishOrder("system_events", "bot_ready", getBotId());
}
```

### On shutdown

While not strictly a lifecycle method like `configure()` or `onStart()`, you can define a method
to react to shutdown requests from the Botica Director. This allows your bot to perform cleanup
tasks or even attempt to cancel a shutdown if it's currently busy (and the shutdown isn't forced).

You can define such a method using the `@ShutdownRequestHandler` annotation.

```java
import es.us.isa.botica.bot.shutdown.ShutdownRequestHandler;

public class MyBotWithShutdownHook extends BaseBot {

  private volatile boolean isBusy = false; // Example internal state

  @ShutdownRequestHandler
  public void onShutdown() {
    System.out.println("Bot is receiving a shutdown request. Performing cleanup...");
    // Perform cleanup like saving state or closing resources.
    // For more advanced handling, including conditional cancellation,
    // refer to the "Handling shutdown requests" documentation.
    saveCurrentState();
  }

  private void saveCurrentState() { /* ... */ }
  // Other bot logic...
}
```

For detailed explanations on shutdown request hooks, like canceling a shutdown, or register hooks
programmatically, please refer to the [Handling shutdown requests](5-handling-shutdown-requests.md)
documentation.

[Back to documentation index](0-index.md)
