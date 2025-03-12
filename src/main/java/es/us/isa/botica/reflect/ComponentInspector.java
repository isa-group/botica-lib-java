package es.us.isa.botica.reflect;

import es.us.isa.botica.bot.Bot;
import es.us.isa.botica.bot.DefaultOrderHandler;
import es.us.isa.botica.bot.OrderHandler;
import es.us.isa.botica.bot.ProactiveTask;
import es.us.isa.botica.bot.shutdown.ShutdownHandler;
import es.us.isa.botica.bot.shutdown.ShutdownRequest;
import es.us.isa.botica.bot.shutdown.ShutdownRequestHandler;
import es.us.isa.botica.bot.shutdown.ShutdownRequestHook;
import es.us.isa.botica.bot.shutdown.ShutdownResponse;
import es.us.isa.botica.protocol.OrderListener;
import java.lang.reflect.Method;
import java.util.List;
import java.util.stream.Collectors;

public final class ComponentInspector {
  private ComponentInspector() {}

  public static void registerHandlerMethods(Bot bot, Class<?> componentClass, Object component) {
    registerOrderHandlers(bot, componentClass, component);
    registerProactiveTask(bot, componentClass, component);
    registerShutdownRequestHandlers(bot, componentClass, component);
  }

  private static void registerOrderHandlers(Bot bot, Class<?> componentClass, Object component) {
    for (Method method : ReflectionUtils.getAllDeclaredMethods(componentClass)) {
      if (method.isAnnotationPresent(DefaultOrderHandler.class)) {
        bot.registerOrderListener(buildOrderListener(component, method));
      }

      if (method.isAnnotationPresent(OrderHandler.class)) {
        String[] orders = method.getAnnotation(OrderHandler.class).value();
        for (String order : orders) {
          bot.registerOrderListener(order, buildOrderListener(component, method));
        }
      }
    }
  }

  private static OrderListener buildOrderListener(Object component, Method method) {
    if (method.getParameterCount() == 0) {
      return (order, message) -> ReflectionUtils.invoke(method, component);
    } else if (method.getParameterCount() == 1
        && method.getParameterTypes()[0].isAssignableFrom(String.class)) {
      return (order, message) -> ReflectionUtils.invoke(method, component, message);
    }

    throw new IllegalStateException(
        String.format(
            "Method %s annotated with @OrderHandler must accept a single String parameter",
            method.toGenericString()));
  }

  private static void registerProactiveTask(Bot bot, Class<?> componentClass, Object component) {
    List<Runnable> tasks =
        ReflectionUtils.getAllDeclaredMethods(componentClass).stream()
            .filter(method -> method.isAnnotationPresent(ProactiveTask.class))
            .map(method -> (Runnable) () -> ReflectionUtils.invoke(method, component))
            .collect(Collectors.toList());

    if (tasks.size() > 1) {
      throw new IllegalStateException(
          "Found more than 1 methods annotated with @ProactiveTask in " + componentClass.getName());
    }

    if (!tasks.isEmpty() && bot.isProactiveTaskSet()) {
      throw new IllegalStateException(
          "Found a method annotated with @ProactiveTask in "
              + componentClass.getName()
              + ", but a proactive task has already been registered manually!");
    }
    tasks.forEach(bot::setProactiveTask);
  }

  private static void registerShutdownRequestHandlers(
      Bot bot, Class<?> componentClass, Object component) {
    ShutdownHandler shutdownHandler = bot.getShutdownHandler();
    ReflectionUtils.getAllDeclaredMethods(componentClass).stream()
        .filter(method -> method.isAnnotationPresent(ShutdownRequestHandler.class))
        .map(method -> buildShutdownRequestHook(component, method))
        .forEach(shutdownHandler::registerShutdownRequestHook);
  }

  private static ShutdownRequestHook buildShutdownRequestHook(Object component, Method method) {
    if (method.getParameterCount() > 1
        || (method.getParameterCount() == 1
            && !ShutdownRequest.class.isAssignableFrom(method.getParameterTypes()[0]))) {
      throw new IllegalStateException(
          String.format(
              "Method %s annotated with @ShutdownRequestHandler must accept a single ShutdownRequest parameter",
              method.toGenericString()));
    }

    if (method.getReturnType() != void.class && method.getReturnType() != ShutdownResponse.class) {
      throw new IllegalStateException(
          String.format(
              "Method %s annotated with @ShutdownRequestHandler must return void or ShutdownResponse",
              method.toGenericString()));
    }
    return (request, response) -> handleShutdownRequest(method, component, request, response);
  }

  private static void handleShutdownRequest(
      Method method, Object component, ShutdownRequest request, ShutdownResponse response) {
    ShutdownResponse returnValue = null;

    if (method.getParameterCount() == 0) {
      ReflectionUtils.invoke(method, component);
    } else {
      Object result = ReflectionUtils.invoke(method, component, request);
      if (result instanceof ShutdownResponse) {
        returnValue = (ShutdownResponse) result;
      }
    }

    if (returnValue != null) {
      response.setCanceled(response.isCanceled() || returnValue.isCanceled());
    }
  }
}
