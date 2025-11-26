# Listening to orders from other bots

In Botica, your bot's subscriptions to order *keys* are defined in the environment
configuration file, not in your bot's code. This means that at runtime, your bot is already
configured to receive orders from specific keys. Your bot's role, then, is to define *listeners*
that react to the *actions* specified within those incoming orders. When an order arrives from any
of its subscribed keys, Botica will execute the appropriate listener bound to that order's action.

## Order handler methods

You can define methods that automatically handle incoming orders based on their action.

Use the `@OrderHandler` annotation on a method to subscribe it to one or more actions. The Botica
runtime automatically invokes this method when an order with a matching action is received.

```java
public class DataProcessorBot extends BaseBot {

  @OrderHandler("process_data")
  public void handleData(String incomingPayload) {
    System.out.println("Processing data: " + incomingPayload);
    String processedResult = process(incomingPayload);
    publishOrder("results_key", "store_processed_results", processedResult);
  }

  private String process(String data) {
    return data.toUpperCase(); // Example processing
  }
}
```

You can specify multiple actions in the `@OrderHandler` annotation as well (e.g.,
`@OrderHandler({"action_a", "action_b"})`).

The method can accept zero or one parameter. If a non-`String` parameter is present, Botica
attempts to deserialize the order's payload into that parameter's type. By default, a `JSONObject`
deserializer is provided and installed:

```java
import es.us.isa.botica.bot.OrderHandler;
import org.json.JSONObject;

public class JsonOrderHandlerBot extends BaseBot {

  @OrderHandler("json_data")
  public void handleJsonData(JSONObject payload) {
    System.out.println("Received JSON data. ID: " + payload.getString("id"));
    String status = processJson(payload);
    publishOrder("json_results", "report_status", new JSONObject().put("status", status));
  }

  private String processJson(JSONObject json) {
    // Example: Read a field and return a status
    return json.has("value") ? "processed-" + json.getString("value") : "processed-no-value";
  }
}
```

For automatic deserialization to any Java type (e.g., custom POJOs or records), you can register the
built-in `JacksonPayloadDeserializer`:

```java
import es.us.isa.botica.bot.BaseBot;
import es.us.isa.botica.bot.OrderHandler;
import es.us.isa.botica.bot.payload.support.JacksonPayloadDeserializer;

public class CustomTypeBot extends BaseBot {

  // Define a custom record for incoming data
  public record Measurement(String sensorId, double value, Instant timestamp) { }

  @Override
  public void configure() {
    // Register Jackson deserializer during the configure phase
    registerPayloadDeserializer(new JacksonPayloadDeserializer());
  }

  @OrderHandler("process_measurement")
  public void processMeasurement(Measurement measurement) {
    String processed = processCustom(measurement);
    publishOrder("custom_results", "custom_report", processed);
  }

  private String processCustom(Measurement measurement) {
    return "Processed: " + measurement.sensorId() + "-" + (measurement.value() * 2);
  }
}
```

For more advanced type deserialization, refer to
the [Payload serializers and deserializers](7-payload-serializers-and-deserializers.md)
documentation.

### Default order handling

The `@DefaultOrderHandler` annotation binds a method to the `defaultAction` property
specified for your bot in the environment configuration file.

Example environment configuration:

```yaml
bots:
  my_reactive_bot_type:
    image: "my_org/my_reactive_bot_image"
    lifecycle:
      type: reactive
      defaultAction: "start_processing" # This is the default action
    # ... other configurations
```

Then, in your bot's code:

```java
import es.us.isa.botica.bot.DefaultOrderHandler;

public class DefaultActionBot extends BaseBot {

  @DefaultOrderHandler
  public void onDefaultAction(String payload) {
    System.out.println("Received default action payload: " + payload);
    String response = createDefaultResponse(payload);
    publishOrder("response_key", "default_response", response);
  }

  private String createDefaultResponse(String input) {
    return "Default response for: " + input;
  }
}
```

> [!NOTE]
> When using IoC frameworks like Spring or Guice, annotation-based configuration might not be
> detected on proxied instances. For best results, instantiate bot classes manually or use factory
> methods that return the actual implementation. Alternatively, order handlers can also be
> registered programmatically, as shown below.

## Registering order listeners programmatically

You can register order listeners programmatically within the `configure()` method. This approach is
beneficial for dynamic listener registration or when integrating with dependency injection
frameworks.

The `registerOrderListener(String action, OrderListener listener)` method binds a functional
`OrderListener` to a specified action.

```java
public class DataProcessorBot extends BaseBot {

  @Override
  public void configure() {
    registerOrderListener("process_data", (action, payload) -> {
      System.out.println("Processing data: " + payload);
      String processedResult = process(payload);
      publishOrder("results_key", "store_processed_results", processedResult);
    });
  }

  private String process(String data) {
    return data.toUpperCase(); // Example processing
  }
}
```

### Registering default order listener programmatically

If your bot has a `defaultAction` defined in its environment configuration, you can register a
listener for it using `registerDefaultOrderListener(OrderListener listener)`.

```java
public class DefaultActionBot extends BaseBot {

  @Override
  public void configure() {
    registerDefaultOrderListener((action, payload) -> {
      System.out.println("Received default action payload: " + payload);
      String response = createDefaultResponse(payload);
      publishOrder("response_key", "default_response", response);
    });
  }

  private String createDefaultResponse(String input) {
    return "Default response for: " + input;
  }
}
```

[Back to documentation index](0-index.md)
