package es.us.isa.botica.bot.payload.support;

import es.us.isa.botica.bot.payload.PayloadSerializer;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.List;
import org.json.JSONObject;

public class JsonObjectPayloadSerializer implements PayloadSerializer<JSONObject> {
  @Override
  public Collection<Type> getSupportedTypes() {
    return List.of(JSONObject.class);
  }

  @Override
  public String serialize(JSONObject object) {
    return object.toString();
  }
}
