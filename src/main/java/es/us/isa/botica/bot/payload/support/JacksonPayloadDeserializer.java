package es.us.isa.botica.bot.payload.support;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;
import es.us.isa.botica.bot.payload.PayloadDeserializer;
import es.us.isa.botica.inspect.Item;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.List;

public class JacksonPayloadDeserializer implements PayloadDeserializer<Object> {
  private final ObjectMapper objectMapper;

  public JacksonPayloadDeserializer() {
    this(new ObjectMapper());
  }

  public JacksonPayloadDeserializer(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  @Override
  public Collection<Type> getSupportedTypes() {
    return List.of(Object.class);
  }

  @Override
  public boolean canDeserialize(Item item, String payload) {
    return true;
  }

  @Override
  public Object deserialize(Item item, String payload) {
    JavaType type = TypeFactory.defaultInstance().constructType(item.getParameterizedType());
    try {
      return objectMapper.readValue(payload, type);
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
  }
}
