# Payload serializers and deserializers

When bots communicate via orders, the actual data (`payload`) is always transmitted as a `String`.
To simplify development, `botica-lib-java` automatically handles the conversion between your Java
objects and these `String` representations.

- **Serialization**: Converts a Java object into a `String` payload when an order is *published*.
- **Deserialization**: Converts a `String` payload into a Java object (for a method parameter) when
  an order is *received*.

## Default payload handling

Out of the box, `botica-lib-java` provides default support for common payload types.

### Default serializers

Used by `publishOrder(key, action, Object payload)`.

- **`String` payloads**: The `String` object is sent directly without any modification.
- **`org.json.JSONObject` payloads**: The `JSONObject` is converted into its `String`
  representation (JSON string).
- **Any other Java type**: The library attempts to serialize the object into a JSON string using an
  internal `JacksonPayloadSerializer`. This means you can often pass custom POJOs (Plain Old Java
  Objects) or records directly, and they will be automatically converted to their JSON string
  equivalent.

### Default deserializers

When an order is received, and its payload needs to be converted into a parameter type for an
`@OrderHandler` method:

- **`String` parameters**: The raw payload string is passed directly to the method.
- **`org.json.JSONObject` parameters**: The payload string is parsed as a JSON object and converted
  into a `JSONObject` instance.

Additionally, for automatically deserializing JSON payloads into any Java type (custom POJOs,
records, etc.), the `JacksonPayloadDeserializer` can be registered manually:

```java
import es.us.isa.botica.bot.BaseBot;
import es.us.isa.botica.bot.OrderHandler;
import es.us.isa.botica.bot.payload.support.JacksonPayloadDeserializer;
import java.time.Instant;

public class CustomPojoDeserializationBot extends BaseBot {

  // Define a custom record that matches an incoming JSON structure
  public record SensorData(String id, double value, Instant timestamp) { }

  @Override
  public void configure() {
    // Register JacksonPayloadDeserializer once to enable JSON-to-Pojo conversion
    registerPayloadDeserializer(new JacksonPayloadDeserializer());
  }

  @OrderHandler("sensor_update")
  public void handleSensorData(SensorData data) {
    System.out.println("Received SensorData:");
    System.out.println("  ID: " + data.id());
    System.out.println("  Value: " + data.value());
    System.out.println("  Timestamp: " + data.timestamp());
    publishOrder("metrics", "log_data", data.id() + ":" + data.value());
  }
}
```

## Custom payload handling

While the default handlers cover many common scenarios, you might need custom logic for specific
data formats or object types. You can extend Botica's payload handling by implementing and
registering your own `PayloadSerializer` and `PayloadDeserializer` instances.

> [!NOTE]
> Custom serializers and deserializers **must be registered during the `configure()` phase** of your
> bot's lifecycle. Registrations made after this phase may not take effect.

### Custom payload deserializers

To deserialize a specific data format into a custom Java object, implement the
`PayloadDeserializer<T>` interface:

```java
import es.us.isa.botica.bot.BaseBot;
import es.us.isa.botica.bot.OrderHandler;
import es.us.isa.botica.bot.payload.PayloadDeserializer;
import es.us.isa.botica.inspect.Item;
import org.json.JSONObject;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.List;

public class ManualJsonDeserializationBot extends BaseBot {

  public record UserProfile(String userId, String emailVerified) { }

  // Custom deserializer for a JSON string that might need manual extraction
  static class ManualUserProfileDeserializer implements PayloadDeserializer<UserProfile> {
    @Override
    public Collection<Type> getSupportedTypes() {
      return List.of(UserProfile.class);
    }

    @Override
    public UserProfile deserialize(Item item, String payload) {
      try {
        JSONObject json = new JSONObject(payload);
        String userId = json.getString("user_id");
        String emailStatus = json.getString("email_status");
        return new UserProfile(userId, emailStatus);
      } catch (Exception e) {
        throw new IllegalArgumentException("Failed to deserialize UserProfile from JSON: " + payload, e);
      }
    }
  }

  @Override
  public void configure() {
    registerPayloadDeserializer(new ManualUserProfileDeserializer());
  }

  @OrderHandler("user_profile_update")
  public void processUserProfile(UserProfile profile) {
    System.out.println("Received UserProfile:");
    System.out.println("  User ID: " + profile.userId());
    System.out.println("  Email Status: " + profile.emailVerified());
    publishOrder("user_management", "update_status", profile.userId());
  }
}
```

You can register deserializers in multiple ways:

- `registerPayloadDeserializer(PayloadDeserializer<?> deserializer)`: The deserializer itself
  defines which types it supports (via `getSupportedTypes()`).
- `registerPayloadDeserializer(Class<T> type, PayloadDeserializer<T> deserializer)`: Explicitly
  binds a deserializer to a specific `Class` type.
- `registerPayloadDeserializer(Type type, PayloadDeserializer<?> deserializer)`: Explicitly binds a
  deserializer to a generic `Type` (useful for complex generics).

### Custom payload serializers

To serialize custom Java objects into a specific string format for publishing, implement the
`PayloadSerializer<T>` interface:

```java
import es.us.isa.botica.bot.BaseBot;
import es.us.isa.botica.bot.ProactiveTask;
import es.us.isa.botica.bot.payload.PayloadSerializer;
import org.json.JSONObject;
import java.lang.reflect.Type;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Base64;

public class CompressedJsonSerializationBot extends BaseBot {

  public record AuditLog(String action, String user, Instant timestamp) { }

  // Custom serializer to convert AuditLog into a Base64 encoded JSON string
  static class Base64JsonAuditLogSerializer implements PayloadSerializer<AuditLog> {
    @Override
    public Collection<Type> getSupportedTypes() {
      return List.of(AuditLog.class);
    }

    @Override
    public String serialize(AuditLog object) {
      try {
        JSONObject json = new JSONObject();
        json.put("action", object.action());
        json.put("user", object.user());
        json.put("timestamp", object.timestamp().toString());
        String rawJson = json.toString();
        return Base64.getEncoder().encodeToString(rawJson.getBytes());
      } catch (Exception e) {
        throw new IllegalArgumentException("Failed to serialize AuditLog to Base64 JSON: " + object, e);
      }
    }
  }

  @Override
  public void configure() {
    registerPayloadSerializer(AuditLog.class, new Base64JsonAuditLogSerializer());
  }

  @ProactiveTask
  public void publishAuditLog() {
    AuditLog log = new AuditLog("login", "admin", Instant.now());
    // This 'log' object will be serialized by Base64JsonAuditLogSerializer
    publishOrder("audit_trail", "new_log_entry", log);
    System.out.println("Published audit log (encoded): " + log);
  }
}
```

You can register serializers in multiple ways:

- `registerPayloadSerializer(PayloadSerializer<?> serializer)`: The serializer itself defines which
  types it supports.
- `registerPayloadSerializer(Class<T> type, PayloadSerializer<T> serializer)`: Explicitly binds a
  serializer to a specific `Class` type.
- `registerPayloadSerializer(Type type, PayloadSerializer<?> serializer)`: Explicitly binds a
  serializer to a generic `Type`.

[Back to documentation index](0-index.md)
