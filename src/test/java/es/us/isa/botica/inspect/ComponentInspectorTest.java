package es.us.isa.botica.inspect;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import es.us.isa.botica.bot.Bot;
import es.us.isa.botica.bot.DefaultOrderHandler;
import es.us.isa.botica.bot.OrderHandler;
import es.us.isa.botica.bot.ProactiveTask;
import es.us.isa.botica.bot.payload.support.JacksonPayloadDeserializer;
import es.us.isa.botica.bot.shutdown.ShutdownHandler;
import es.us.isa.botica.bot.shutdown.ShutdownRequest;
import es.us.isa.botica.bot.shutdown.ShutdownRequestHandler;
import es.us.isa.botica.bot.shutdown.ShutdownRequestHook;
import es.us.isa.botica.bot.shutdown.ShutdownResponse;
import es.us.isa.botica.protocol.OrderListener;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoSettings;

@MockitoSettings
class ComponentInspectorTest {
  @Mock private Bot mockBot;
  @Mock private ShutdownHandler mockShutdownHandler;

  @Captor private ArgumentCaptor<Runnable> proactiveTaskCaptor;
  @Captor private ArgumentCaptor<OrderListener> orderListenerCaptor;
  @Captor private ArgumentCaptor<ShutdownRequestHook> shutdownHookCaptor;

  private ComponentInspector inspector;

  private static class ProactiveBot {
    boolean taskExecuted = false;

    @ProactiveTask
    public void myProactiveTask() {
      taskExecuted = true;
    }
  }

  private static class OrderHandlerBot {
    String receivedPayload = null;

    @OrderHandler("action1")
    public void handleAction1(String payload) {
      receivedPayload = "action1:" + payload;
    }

    @OrderHandler({"action2", "action3"})
    public void handleMultipleActions() {
      receivedPayload = "multiple";
    }
  }

  private static class DefaultOrderHandlerBot {
    String receivedPayload = null;

    @DefaultOrderHandler
    public void handleDefault(String payload) {
      receivedPayload = "default:" + payload;
    }
  }

  private static class VoidShutdownHandlerBot {
    boolean hookExecuted = false;

    @ShutdownRequestHandler
    public void onShutdown(ShutdownRequest request) {
      hookExecuted = true;
    }
  }

  private static class ShutdownHandlerWithResponseBot {
    boolean hookExecuted = false;

    @ShutdownRequestHandler
    public ShutdownResponse onShutdown(ShutdownRequest request) {
      hookExecuted = true;
      return ShutdownResponse.cancel();
    }
  }

  private static class InvalidOrderHandlerBot {
    @OrderHandler("invalid")
    public void handle(String p1, String p2) {}
  }

  private static class InvalidProactiveTaskBot {
    @ProactiveTask
    public void task1() {}

    @ProactiveTask
    public void task2() {}
  }

  private static class MyData {
    public String value;
  }

  private static class DeserializerBot {
    MyData receivedData;

    @OrderHandler("deserialize")
    public void handle(MyData data) {
      receivedData = data;
    }
  }

  @BeforeEach
  void setUp() {
    inspector = new ComponentInspector();
    lenient().when(mockBot.getShutdownHandler()).thenReturn(mockShutdownHandler);
  }

  @Test
  @DisplayName("registerHandlerMethods should register a @ProactiveTask method")
  void registerHandlerMethods_registersProactiveTask() {
    // Arrange
    ProactiveBot botImpl = new ProactiveBot();
    when(mockBot.isProactiveTaskSet()).thenReturn(false);

    // Act
    inspector.registerHandlerMethods(mockBot, botImpl.getClass(), botImpl);

    // Assert
    verify(mockBot, times(1)).setProactiveTask(proactiveTaskCaptor.capture());
    assertThat(botImpl.taskExecuted).isFalse();

    // Verify the captured task works
    proactiveTaskCaptor.getValue().run();
    assertThat(botImpl.taskExecuted).isTrue();
  }

  @Test
  @DisplayName("registerHandlerMethods should throw exception for multiple @ProactiveTask methods")
  void registerHandlerMethods_multipleProactiveTasks_throwsException() {
    // Arrange
    InvalidProactiveTaskBot botImpl = new InvalidProactiveTaskBot();

    // Act & Assert
    assertThatThrownBy(() -> inspector.registerHandlerMethods(mockBot, botImpl.getClass(), botImpl))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("Found more than 1 methods annotated with @ProactiveTask");
  }

  @Test
  @DisplayName("registerHandlerMethods should register @OrderHandler methods for each action")
  void registerHandlerMethods_registersOrderHandlers() {
    // Arrange
    OrderHandlerBot botImpl = new OrderHandlerBot();

    // Act
    inspector.registerHandlerMethods(mockBot, botImpl.getClass(), botImpl);

    // Assert
    verify(mockBot, times(1)).registerOrderListener(eq("action1"), any(OrderListener.class));
    verify(mockBot, times(1)).registerOrderListener(eq("action2"), any(OrderListener.class));
    verify(mockBot, times(1)).registerOrderListener(eq("action3"), any(OrderListener.class));
  }

  @Test
  @DisplayName("registerHandlerMethods should register a @DefaultOrderHandler method")
  void registerHandlerMethods_registersDefaultOrderHandler() {
    // Arrange
    DefaultOrderHandlerBot botImpl = new DefaultOrderHandlerBot();

    // Act
    inspector.registerHandlerMethods(mockBot, botImpl.getClass(), botImpl);

    // Assert
    verify(mockBot, times(1)).registerDefaultOrderListener(orderListenerCaptor.capture());

    // Verify the listener works
    orderListenerCaptor.getValue().onMessageReceived("defaultAction", "testPayload");
    assertThat(botImpl.receivedPayload).isEqualTo("default:testPayload");
  }

  @Test
  @DisplayName(
      "registerHandlerMethods should register a void @ShutdownRequestHandler method")
  void registerHandlerMethods_registersVoidShutdownRequestHandler() {
    // Arrange
    VoidShutdownHandlerBot botImpl = new VoidShutdownHandlerBot();

    // Act
    inspector.registerHandlerMethods(mockBot, botImpl.getClass(), botImpl);

    // Assert
    verify(mockShutdownHandler, times(1)).registerShutdownRequestHook(shutdownHookCaptor.capture());

    // Verify the hook works
    ShutdownRequest request = new ShutdownRequest(false);
    ShutdownResponse response = new ShutdownResponse();
    shutdownHookCaptor.getValue().onShutdownRequest(request, response);
    assertThat(botImpl.hookExecuted).isTrue();
    assertThat(response.isCanceled()).isFalse();
  }

  @Test
  @DisplayName(
      "registerHandlerMethods should register a @ShutdownRequestHandler method returning ShutdownResponse")
  void registerHandlerMethods_registersShutdownRequestHandlerWithResponse() {
    // Arrange
    ShutdownHandlerWithResponseBot botImpl = new ShutdownHandlerWithResponseBot();

    // Act
    inspector.registerHandlerMethods(mockBot, botImpl.getClass(), botImpl);

    // Assert
    verify(mockShutdownHandler, times(1)).registerShutdownRequestHook(shutdownHookCaptor.capture());

    // Verify the hook works
    ShutdownRequest request = new ShutdownRequest(false);
    ShutdownResponse response = new ShutdownResponse();
    shutdownHookCaptor.getValue().onShutdownRequest(request, response);
    assertThat(botImpl.hookExecuted).isTrue();
    assertThat(response.isCanceled()).isTrue();
  }

  @Test
  @DisplayName(
      "registerHandlerMethods should throw exception for @OrderHandler with invalid parameters")
  void registerHandlerMethods_invalidOrderHandlerParams_throwsException() {
    // Arrange
    InvalidOrderHandlerBot botImpl = new InvalidOrderHandlerBot();

    // Act & Assert
    assertThatThrownBy(() -> inspector.registerHandlerMethods(mockBot, botImpl.getClass(), botImpl))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("must accept none or just one parameter");
  }

  @Test
  @DisplayName("OrderListener should correctly deserialize payload for handler methods")
  void orderListener_deserializesPayloadCorrectly() {
    // Arrange
    DeserializerBot botImpl = new DeserializerBot();
    // Register the powerful Jackson deserializer for this test
    inspector.registerPayloadDeserializer(new JacksonPayloadDeserializer());

    // Act
    inspector.registerHandlerMethods(mockBot, botImpl.getClass(), botImpl);

    // Assert
    verify(mockBot, times(1))
        .registerOrderListener(eq("deserialize"), orderListenerCaptor.capture());

    // Now, invoke the captured listener to simulate an incoming message
    orderListenerCaptor.getValue().onMessageReceived("deserialize", "{\"value\":\"test\"}");

    // Verify the payload was correctly deserialized and the method was called
    assertThat(botImpl.receivedData).isNotNull();
    assertThat(botImpl.receivedData.value).isEqualTo("test");
  }
}
