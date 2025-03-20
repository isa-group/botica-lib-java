package es.us.isa.botica.bot.order;

import es.us.isa.botica.inspect.Item;
import java.lang.reflect.Type;
import java.util.Collection;

/**
 * Converts string messages to typed objects for {@code @OrderHandler} methods.
 *
 * <p>Implementations of this interface are responsible for determining whether they can convert a
 * particular message for a given parameter type, and for performing the actual conversion.
 *
 * <p>When an {@code @OrderHandler} method is invoked with a message, the framework will:
 *
 * <ol>
 *   <li>First check for an exact type match in the registered converters
 *   <li>If no exact match is found, query each registered converter via {@link #canConvert} to see
 *       if it can handle the parameter type and message
 *   <li>Use the first compatible converter to transform the message via {@link #convert}
 * </ol>
 *
 * @param <T> the type that this converter produces
 */
public interface OrderMessageTypeConverter<T> {
  /**
   * Returns the collection of types this converter can handle.
   *
   * @return a collection of {@link Type} objects this converter supports
   */
  Collection<Type> getSupportedTypes();

  /**
   * Determines whether this converter can handle the given parameter and message.
   *
   * <p>The default implementation checks if the parameter's type is contained in the collection
   * returned by {@link #getSupportedTypes()}.
   *
   * <p>Override this method to implement more specific conversion logic based on the message
   * content or parameter metadata.
   *
   * @param item the parameter item to convert the message for
   * @param message the message to be converted
   * @return {@code true} if this converter can handle the conversion, {@code false} otherwise
   */
  default boolean canConvert(Item item, String message) {
    return getSupportedTypes().contains(item.getParameterizedType());
  }

  /**
   * Converts the given message string to an object of type {@code T}.
   *
   * @param message the message string to convert
   * @return the converted object
   * @throws IllegalArgumentException if the message cannot be converted to the target type
   */
  T convert(Item item, String message);
}
