package es.us.isa.botica.bot.order;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;
import es.us.isa.botica.inspect.Item;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.List;

public class JacksonOrderMessageTypeConverter implements OrderMessageTypeConverter<Object> {
  private final ObjectMapper objectMapper;

  public JacksonOrderMessageTypeConverter() {
    this(new ObjectMapper());
  }

  public JacksonOrderMessageTypeConverter(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  @Override
  public Collection<Type> getSupportedTypes() {
    return List.of(Object.class);
  }

  @Override
  public boolean canConvert(Item item, String message) {
    return true;
  }

  @Override
  public Object convert(Item item, String message) {
    JavaType type = TypeFactory.defaultInstance().constructType(item.getParameterizedType());
    try {
      return objectMapper.readValue(message, type);
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
  }
}
