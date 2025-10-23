package es.us.isa.botica.bot.payload.support;

import es.us.isa.botica.bot.payload.PayloadSerializer;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.List;

public class StringPayloadSerializer implements PayloadSerializer<String> {
  @Override
  public Collection<Type> getSupportedTypes() {
    return List.of(String.class);
  }

  @Override
  public String serialize(String object) {
    return object;
  }
}
