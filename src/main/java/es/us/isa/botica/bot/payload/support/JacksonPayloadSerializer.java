package es.us.isa.botica.bot.payload.support;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import es.us.isa.botica.bot.payload.PayloadSerializer;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.List;

public class JacksonPayloadSerializer implements PayloadSerializer<Object> {
  private final ObjectMapper objectMapper;

  public JacksonPayloadSerializer() {
    this(new ObjectMapper());
  }

  public JacksonPayloadSerializer(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  @Override
  public Collection<Type> getSupportedTypes() {
    return List.of(Object.class);
  }

  @Override
  public boolean canSerialize(Object object) {
    return true;
  }

  @Override
  public String serialize(Object object) {
    try {
      return objectMapper.writeValueAsString(object);
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
  }
}
