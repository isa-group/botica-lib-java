package es.us.isa.botica.inspect;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;

/** Abstraction for {@link java.lang.reflect.Parameter} and {@link java.lang.reflect.Field}. */
public class Item implements AnnotatedElement {
  private final Class<?> type;
  private final Type parameterizedType;
  private final AnnotatedElement annotatedElement;

  public static Item fromParameter(Parameter parameter) {
    return new Item(parameter.getType(), parameter.getParameterizedType(), parameter);
  }

  public Item(Class<?> type, Type parameterizedType, AnnotatedElement annotatedElement) {
    this.type = type;
    this.parameterizedType = parameterizedType;
    this.annotatedElement = annotatedElement;
  }

  public Class<?> getType() {
    return type;
  }

  public Type getParameterizedType() {
    return parameterizedType;
  }

  @Override
  public <T extends Annotation> T getAnnotation(Class<T> annotationClass) {
    return annotationClass.getAnnotation(annotationClass);
  }

  @Override
  public Annotation[] getAnnotations() {
    return annotatedElement.getAnnotations();
  }

  @Override
  public Annotation[] getDeclaredAnnotations() {
    return annotatedElement.getDeclaredAnnotations();
  }
}
