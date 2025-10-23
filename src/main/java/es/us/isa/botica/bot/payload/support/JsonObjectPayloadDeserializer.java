package es.us.isa.botica.bot.payload.support;

import es.us.isa.botica.bot.payload.PayloadDeserializer;
import es.us.isa.botica.inspect.Item;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.List;
import org.json.JSONObject;

public class JsonObjectPayloadDeserializer implements PayloadDeserializer<JSONObject> {
  @Override
  public Collection<Type> getSupportedTypes() {
    return List.of(JSONObject.class);
  }

  @Override
  public JSONObject deserialize(Item item, String payload) {
    return new JSONObject(payload);
  }
}
