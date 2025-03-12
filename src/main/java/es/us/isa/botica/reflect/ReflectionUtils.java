package es.us.isa.botica.reflect;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class ReflectionUtils {
  private ReflectionUtils() {}

  public static List<Method> getAllDeclaredMethods(Class<?> clazz) {
    if (clazz == null || clazz.equals(Object.class)) {
      return new ArrayList<>();
    }

    List<Method> methods = getAllDeclaredMethods(clazz.getSuperclass());
    methods.addAll(Arrays.asList(clazz.getDeclaredMethods()));

    return methods;
  }

  public static Object invoke(Method method, Object object, Object... params) {
    try {
      return method.invoke(object, params);
    } catch (IllegalAccessException | InvocationTargetException e) {
      throw new RuntimeException(e);
    }
  }
}
