package es.us.isa.botica.bot.payload;

import es.us.isa.botica.inspect.Item;
import java.lang.reflect.Type;
import java.util.Collection;

/**
 * Converts string payloads to typed objects for {@code @OrderHandler} methods.
 *
 * <p>Implementations of this interface are responsible for determining whether they can deserialize
 * a particular payload for a given parameter type, and for performing the actual deserialization.
 *
 * @param <T> the type that this deserializer produces
 */
public interface PayloadDeserializer<T> {
  /**
   * Returns the collection of types this deserializer can handle.
   *
   * @return a collection of {@link Type} objects this deserializer supports
   */
  Collection<Type> getSupportedTypes();

  /**
   * Determines whether this deserializer can handle the given parameter and payload.
   *
   * <p>The default implementation checks if the parameter's type is contained in the collection
   * returned by {@link #getSupportedTypes()}.
   *
   * @param item the parameter item to deserialize the payload for
   * @param payload the payload to be deserialized
   * @return {@code true} if this deserializer can handle the deserialization, {@code false}
   *     otherwise
   */
  default boolean canDeserialize(Item item, String payload) {
    return getSupportedTypes().contains(item.getParameterizedType());
  }

  /**
   * Deserializes the given payload string to an object of type {@code T}.
   *
   * @param item the parameter item containing type information
   * @param payload the payload string to deserialize
   * @return the deserialized object
   * @throws IllegalArgumentException if the payload cannot be deserialized to the target type
   */
  T deserialize(Item item, String payload);
}
