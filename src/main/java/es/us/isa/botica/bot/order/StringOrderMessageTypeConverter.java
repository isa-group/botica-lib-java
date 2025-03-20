package es.us.isa.botica.bot.order;

import es.us.isa.botica.inspect.Item;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.List;

public class StringOrderMessageTypeConverter implements OrderMessageTypeConverter<String> {
  @Override
  public Collection<Type> getSupportedTypes() {
    return List.of(String.class);
  }

  @Override
  public String convert(Item item, String message) {
    return message;
  }
}
