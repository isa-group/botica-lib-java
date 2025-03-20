package es.us.isa.botica.inspect;

import es.us.isa.botica.bot.Bot;
import es.us.isa.botica.bot.DefaultOrderHandler;
import es.us.isa.botica.bot.OrderHandler;
import es.us.isa.botica.bot.ProactiveTask;
import es.us.isa.botica.bot.order.JsonObjectOrderMessageTypeConverter;
import es.us.isa.botica.bot.order.OrderMessageTypeConverter;
import es.us.isa.botica.bot.order.StringOrderMessageTypeConverter;
import es.us.isa.botica.bot.shutdown.ShutdownHandler;
import es.us.isa.botica.bot.shutdown.ShutdownRequest;
import es.us.isa.botica.bot.shutdown.ShutdownRequestHandler;
import es.us.isa.botica.bot.shutdown.ShutdownRequestHook;
import es.us.isa.botica.bot.shutdown.ShutdownResponse;
import es.us.isa.botica.protocol.OrderListener;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ComponentInspector {
  private final Map<Type, OrderMessageTypeConverter<?>> orderMessageTypeConverters;

  public ComponentInspector() {
    this.orderMessageTypeConverters = new HashMap<>();
    this.registerOrderMessageTypeConverter(new StringOrderMessageTypeConverter());
    this.registerOrderMessageTypeConverter(new JsonObjectOrderMessageTypeConverter());
  }

  public void registerOrderMessageTypeConverter(OrderMessageTypeConverter<?> converter) {
    converter
        .getSupportedTypes()
        .forEach(type -> this.registerOrderMessageTypeConverter(type, converter));
  }

  public <T> void registerOrderMessageTypeConverter(
      Class<? super T> type, OrderMessageTypeConverter<T> converter) {
    this.registerOrderMessageTypeConverter((Type) type, converter);
  }

  public void registerOrderMessageTypeConverter(Type type, OrderMessageTypeConverter<?> converter) {
    this.orderMessageTypeConverters.put(type, converter);
  }

  public void registerHandlerMethods(Bot bot, Class<?> componentClass, Object component) {
    registerOrderHandlers(bot, componentClass, component);
    registerProactiveTask(bot, componentClass, component);
    registerShutdownRequestHandlers(bot, componentClass, component);
  }

  private void registerOrderHandlers(Bot bot, Class<?> componentClass, Object component) {
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

  private OrderListener buildOrderListener(Object component, Method method) {
    if (method.getParameterCount() == 0) {
      return (order, message) -> ReflectionUtils.invoke(method, component);
    } else if (method.getParameterCount() == 1) {
      return (order, message) -> {
        this.invokeOrderHandlerMethod(component, method, message);
      };
    }

    throw new IllegalStateException(
        String.format(
            "Method %s annotated with @OrderHandler must accept none or just one parameter",
            method.toGenericString()));
  }

  private void invokeOrderHandlerMethod(Object component, Method method, String message) {
    Parameter parameter = method.getParameters()[0];
    Item item = Item.fromParameter(parameter);
    Type type = item.getParameterizedType();

    OrderMessageTypeConverter<?> converter = orderMessageTypeConverters.get(type);
    if (converter != null) {
      ReflectionUtils.invoke(method, component, converter.convert(item, message));
      return;
    }

    for (OrderMessageTypeConverter<?> registeredConverter : orderMessageTypeConverters.values()) {
      if (registeredConverter.canConvert(item, message)) {
        ReflectionUtils.invoke(method, component, registeredConverter.convert(item, message));
        return;
      }
    }

    throw new IllegalStateException(
        String.format(
            "No type converter found for the %s parameter in method %s",
            parameter, parameter.getDeclaringExecutable().toGenericString()));
  }

  private void registerProactiveTask(Bot bot, Class<?> componentClass, Object component) {
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

  private void registerShutdownRequestHandlers(Bot bot, Class<?> componentClass, Object component) {
    ShutdownHandler shutdownHandler = bot.getShutdownHandler();
    ReflectionUtils.getAllDeclaredMethods(componentClass).stream()
        .filter(method -> method.isAnnotationPresent(ShutdownRequestHandler.class))
        .map(method -> buildShutdownRequestHook(component, method))
        .forEach(shutdownHandler::registerShutdownRequestHook);
  }

  private ShutdownRequestHook buildShutdownRequestHook(Object component, Method method) {
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

  private void handleShutdownRequest(
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
