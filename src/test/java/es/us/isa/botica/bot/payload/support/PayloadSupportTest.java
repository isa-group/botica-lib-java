package es.us.isa.botica.bot.payload.support;

import static org.assertj.core.api.Assertions.assertThat;

import es.us.isa.botica.inspect.Item;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import org.json.JSONObject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("Payload Support Utilities Tests")
class PayloadSupportTest {
  private static class MyPojo {
    public String name;
  }

  private static class TestClass {
    public void method(String s) {}

    public void method(JSONObject o) {}

    public void method(MyPojo p) {}
  }

  private Item createItemForType(Class<?> cls) throws NoSuchMethodException {
    Method testMethod = TestClass.class.getMethod("method", cls);
    Parameter parameter = testMethod.getParameters()[0];
    return Item.fromParameter(parameter);
  }

  @Nested
  @DisplayName("for StringPayloadSerializer")
  class StringPayloadSerializerTest {
    @Test
    @DisplayName("serialize should return the string itself")
    void serialize_returnsString() {
      // Arrange
      StringPayloadSerializer serializer = new StringPayloadSerializer();
      String input = "Hello World";

      // Act & Assert
      assertThat(serializer.serialize(input)).isEqualTo(input);
    }
  }

  @Nested
  @DisplayName("for StringPayloadDeserializer")
  class StringPayloadDeserializerTest {
    @Test
    @DisplayName("deserialize should return the string payload itself")
    void deserialize_returnsPayload() throws NoSuchMethodException {
      // Arrange
      StringPayloadDeserializer deserializer = new StringPayloadDeserializer();
      String payload = "Hello World";
      Item item = createItemForType(String.class);

      // Act & Assert
      assertThat(deserializer.deserialize(item, payload)).isEqualTo(payload);
    }
  }

  @Nested
  @DisplayName("for JsonObjectPayloadSerializer")
  class JsonObjectPayloadSerializerTest {
    @Test
    @DisplayName("serialize should return the JSON string representation")
    void serialize_returnsJsonString() {
      // Arrange
      JsonObjectPayloadSerializer serializer = new JsonObjectPayloadSerializer();
      JSONObject json = new JSONObject();
      json.put("key", "value");

      // Act & Assert
      assertThat(serializer.serialize(json)).isEqualTo("{\"key\":\"value\"}");
    }
  }

  @Nested
  @DisplayName("for JsonObjectPayloadDeserializer")
  class JsonObjectPayloadDeserializerTest {
    @Test
    @DisplayName("deserialize should parse the string into a JSONObject")
    void deserialize_parsesStringToJsonObject() throws NoSuchMethodException {
      // Arrange
      JsonObjectPayloadDeserializer deserializer = new JsonObjectPayloadDeserializer();
      String payload = "{\"key\":\"value\"}";
      Item item = createItemForType(JSONObject.class);

      // Act
      JSONObject result = deserializer.deserialize(item, payload);

      // Assert
      assertThat(result.getString("key")).isEqualTo("value");
    }
  }

  @Nested
  @DisplayName("for JacksonPayloadSerializer")
  class JacksonPayloadSerializerTest {
    @Test
    @DisplayName("serialize should convert a POJO to its JSON string representation")
    void serialize_pojo_returnsJsonString() {
      // Arrange
      JacksonPayloadSerializer serializer = new JacksonPayloadSerializer();
      MyPojo pojo = new MyPojo();
      pojo.name = "test-name";

      // Act & Assert
      assertThat(serializer.serialize(pojo)).isEqualTo("{\"name\":\"test-name\"}");
    }
  }

  @Nested
  @DisplayName("for JacksonPayloadDeserializer")
  class JacksonPayloadDeserializerTest {
    @Test
    @DisplayName("deserialize should parse a JSON string into a POJO")
    void deserialize_jsonString_returnsPojo() throws NoSuchMethodException {
      // Arrange
      JacksonPayloadDeserializer deserializer = new JacksonPayloadDeserializer();
      String payload = "{\"name\":\"test-name\"}";
      Item item = createItemForType(MyPojo.class);

      // Act
      Object result = deserializer.deserialize(item, payload);

      // Assert
      assertThat(result).isInstanceOf(MyPojo.class);
      assertThat(((MyPojo) result).name).isEqualTo("test-name");
    }
  }
}
