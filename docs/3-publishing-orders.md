# Publishing orders

Publishing orders is a fundamental way for bots to initiate actions and share data. Bots publish
orders through the `publishOrder(String key, String action, Object payload)` method provided by
`BaseBot`:

```java
import es.us.isa.botica.bot.BaseBot;
import org.json.JSONObject;
import java.time.Instant;
import java.util.UUID;

public class DataProducerBot extends BaseBot {

  @ProactiveTask
  public void sendMeasurement() {
    JSONObject data = new JSONObject()
        .put("sensor_id", "sensor-101")
        .put("value", Math.random() * 100)
        .put("timestamp", Instant.now());

    publishOrder("measurements_key", "record_measurement", data);
  }
}
```

An order always consists of 3 different parts:

- **Key**: A broker-level concept that determines which bots will receive the order based on their
  subscriptions defined in the environment file.
- **Action**: A bot-level concept used by receiving bots to route the order to a specific handler
  method or listener.
- **Payload**: The actual data being sent. It can be any Java `Object`.

When you publish an order, the `Object` payload is automatically serialized into a `String` before
being sent to the message broker. The `botica-lib-java` library comes with built-in support for
common payload types:

- **`String`**: Passed directly without modification.
- **`org.json.JSONObject`**: Converted to its JSON string representation.
- **Any other Java type**: Automatically serialized to a JSON string using an internal
  `JacksonPayloadSerializer`. This allows you to easily publish custom POJOs or records:

    ```java
    import es.us.isa.botica.bot.BaseBot;
    import es.us.isa.botica.bot.ProactiveTask;
    
    public class CustomPayloadPublisherBot extends BaseBot {
    
      public record Measurement(String sensorId, double value, Instant timestamp) { }
    
      @ProactiveTask
      public void sendMeasurement() {
        Measurement data = new Measurement("sensor-101", Math.random() * 100, Instant.now());
        publishOrder("measurements_key", "record_measurement", data);
      }
    }
    ```

### Custom payload serialization

You can register your own `PayloadSerializer` implementations during the `configure()` phase to
handle custom serialization logic for specific types. This gives you full control over how your
objects are converted to strings for publishing. For more details on custom serialization, refer to
the [Payload serializers and deserializers](7-payload-serializers-and-deserializers.md)
documentation.

## Publishing orders with defaults

To streamline development, you can configure default keys and actions in your bot's environment
file. This allows you to publish orders without explicitly specifying these details every time.

Example environment configuration for publish defaults:

```yaml
bots:
  my_producer_bot_type:
    image: "my_org/my_producer_bot_image"
    publish:
      defaultKey: "default_output"
      defaultAction: "log_event"
    # ... other configurations
```

With these defaults configured, you can use the `publishDefaultOrder(Object payload)` method.

```java
import es.us.isa.botica.bot.BaseBot;
import es.us.isa.botica.bot.ProactiveTask;

public class DefaultPublisherBot extends BaseBot {

  @ProactiveTask
  public void sendDefaultEvent() {
    String eventPayload = "Event from " + getBotId() + " at " + Instant.now();
    // Publishes using 'default_output' key and 'log_event' action
    publishDefaultOrder(eventPayload);
    System.out.println("Published default event: " + eventPayload);
  }
}
```

This method will automatically use the `defaultKey` and `defaultAction` defined in your bot's
environment configuration. If no defaults are configured, calling this method will throw an
`IllegalStateException`.

[Back to documentation index](0-index.md)
