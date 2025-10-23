package es.us.isa.botica.bot.payload.support;

import es.us.isa.botica.bot.payload.PayloadDeserializer;
import es.us.isa.botica.inspect.Item;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.List;

public class StringPayloadDeserializer implements PayloadDeserializer<String> {
  @Override
  public Collection<Type> getSupportedTypes() {
    return List.of(String.class);
  }

  @Override
  public String deserialize(Item item, String payload) {
    return payload;
  }
}
