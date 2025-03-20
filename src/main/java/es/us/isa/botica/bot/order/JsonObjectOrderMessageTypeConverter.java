package es.us.isa.botica.bot.order;

import es.us.isa.botica.inspect.Item;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.List;
import org.json.JSONObject;

public class JsonObjectOrderMessageTypeConverter implements OrderMessageTypeConverter<JSONObject> {
  @Override
  public Collection<Type> getSupportedTypes() {
    return List.of(JSONObject.class);
  }

  @Override
  public JSONObject convert(Item item, String message) {
    return new JSONObject(message);
  }
}
