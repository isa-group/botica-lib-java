package es.us.isa.botica.bot.payload;

import java.lang.reflect.Type;
import java.util.Collection;

/**
 * Serializes objects to string payloads for publishing orders.
 *
 * <p>Implementations of this interface are responsible for determining whether they can serialize a
 * particular object type, and for performing the actual serialization.
 *
 * @param <T> the type that this serializer can handle
 */
public interface PayloadSerializer<T> {
  /**
   * Returns the collection of types this serializer can handle.
   *
   * @return a collection of {@link Type} objects this serializer supports
   */
  Collection<Type> getSupportedTypes();

  /**
   * Determines whether this serializer can handle the given object.
   *
   * <p>The default implementation checks if the object's type is assignable to any of the types
   * returned by {@link #getSupportedTypes()}.
   *
   * @param object the object to be serialized
   * @return {@code true} if this serializer can handle the serialization, {@code false} otherwise
   */
  default boolean canSerialize(Object object) {
    if (object == null) {
      return false;
    }
    return getSupportedTypes().stream()
        .filter(type -> type instanceof Class<?>)
        .map(type -> (Class<?>) type)
        .anyMatch(clazz -> clazz.isInstance(object));
  }

  /**
   * Serializes the given object to a string payload.
   *
   * @param object the object to serialize
   * @return the serialized string payload
   * @throws IllegalArgumentException if the object cannot be serialized
   */
  String serialize(T object);
}
