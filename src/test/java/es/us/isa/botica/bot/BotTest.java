package es.us.isa.botica.bot;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import es.us.isa.botica.configuration.bot.BotInstanceConfiguration;
import es.us.isa.botica.configuration.bot.BotPublishConfiguration;
import es.us.isa.botica.configuration.bot.BotTypeConfiguration;
import es.us.isa.botica.configuration.bot.lifecycle.BotLifecycleType;
import es.us.isa.botica.configuration.bot.lifecycle.ProactiveBotLifecycleConfiguration;
import es.us.isa.botica.protocol.BoticaClient;
import es.us.isa.botica.protocol.client.ReadyPacket;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoSettings;

@MockitoSettings
class BotTest {
  @Mock private BoticaClient mockBoticaClient;
  @Mock private BotInstanceConfiguration mockBotConfig;
  @Mock private BotTypeConfiguration mockBotTypeConfig;
  @Mock private ProactiveBotLifecycleConfiguration mockProactiveConfig;
  @Mock private ScheduledExecutorService mockExecutorService;

  @Captor private ArgumentCaptor<Runnable> scheduledTaskCaptor;
  @Captor private ArgumentCaptor<Long> initialDelayCaptor;
  @Captor private ArgumentCaptor<Long> periodCaptor;

  private Bot bot;

  @BeforeEach
  void setUp() {
    // Standard setup for most tests
    lenient().when(mockBotConfig.getTypeConfiguration()).thenReturn(mockBotTypeConfig);
    lenient().when(mockBotConfig.getLifecycleConfiguration()).thenReturn(mockProactiveConfig);
    lenient().when(mockBotConfig.getId()).thenReturn("test-bot");
    bot = new Bot(mockBoticaClient, mockBotConfig);
  }

  // --- Lifecycle Tests ---

  @Test
  @DisplayName("start should connect client, send ReadyPacket, and set running state")
  void start_connectsAndSendsReadyPacket() throws TimeoutException {
    // Arrange
    when(mockProactiveConfig.getType()).thenReturn(BotLifecycleType.REACTIVE); // Not proactive

    // Act
    bot.start();

    // Assert
    verify(mockBoticaClient, times(1)).connect();
    verify(mockBoticaClient, times(1)).sendPacket(any(ReadyPacket.class));
    assertThat(bot.isRunning()).isTrue();
  }

  @Test
  @DisplayName("stop should close client and reset running state")
  void stop_closesClientAndResetsState() throws TimeoutException {
    // Arrange
    bot.start(); // First, start the bot
    assertThat(bot.isRunning()).isTrue();

    // Act
    bot.stop();

    // Assert
    verify(mockBoticaClient, times(1)).close();
    assertThat(bot.isRunning()).isFalse();
  }

  @Test
  @DisplayName("stop should throw IllegalStateException if called when not running")
  void stop_notRunning_throwsException() {
    // Act & Assert
    assertThatThrownBy(() -> bot.stop())
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("bot is not running");
  }

  // --- Proactive Task Tests ---

  @Test
  @DisplayName("setProactiveTask should throw exception if bot is not proactive")
  void setProactiveTask_notProactive_throwsException() {
    // Arrange
    when(mockProactiveConfig.getType()).thenReturn(BotLifecycleType.REACTIVE);

    // Act & Assert
    assertThatThrownBy(() -> bot.setProactiveTask(() -> {}))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("not configured as proactive");
  }

  @Test
  @DisplayName("start should throw exception if proactive but task is not set")
  void start_proactiveButNoTask_throwsException() {
    // Arrange
    when(mockProactiveConfig.getType()).thenReturn(BotLifecycleType.PROACTIVE);

    // Act & Assert
    assertThatThrownBy(() -> bot.start())
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("no proactive task has been registered");
  }

  @Test
  @DisplayName("start should schedule a repeating proactive task correctly")
  void start_proactiveRepeating_schedulesCorrectly() throws TimeoutException {
    // Arrange
    when(mockProactiveConfig.getType()).thenReturn(BotLifecycleType.PROACTIVE);
    when(mockProactiveConfig.getInitialDelay()).thenReturn(10L);
    when(mockProactiveConfig.getPeriod()).thenReturn(30L);
    bot.setProactiveTask(() -> {});

    try (MockedStatic<Executors> mockedExecutors = Mockito.mockStatic(Executors.class)) {
      mockedExecutors
          .when(Executors::newSingleThreadScheduledExecutor)
          .thenReturn(mockExecutorService);

      // Act
      bot.start();

      // Assert
      verify(mockExecutorService, times(1))
          .scheduleWithFixedDelay(
              scheduledTaskCaptor.capture(),
              initialDelayCaptor.capture(),
              periodCaptor.capture(),
              eq(TimeUnit.SECONDS));

      assertThat(initialDelayCaptor.getValue()).isEqualTo(10L);
      assertThat(periodCaptor.getValue()).isEqualTo(30L);
    }
  }

  // --- Publishing Tests ---

  @Test
  @DisplayName("publishDefaultOrder should use key and action from configuration")
  void publishDefaultOrder_usesConfigValues() {
    // Arrange
    BotPublishConfiguration publishConfig = mock(BotPublishConfiguration.class);
    when(mockBotTypeConfig.getPublishConfiguration()).thenReturn(publishConfig);
    when(publishConfig.getDefaultKey()).thenReturn("default-key");
    when(publishConfig.getDefaultAction()).thenReturn("default-action");

    // Act
    bot.publishDefaultOrder("my-payload");

    // Assert
    verify(mockBoticaClient, times(1))
        .publishOrder(eq("default-key"), eq("default-action"), eq("my-payload"));
  }

  @Test
  @DisplayName("publishDefaultOrder should throw exception if defaults are not configured")
  void publishDefaultOrder_missingDefaults_throwsException() {
    // Arrange
    BotPublishConfiguration publishConfig = mock(BotPublishConfiguration.class);
    when(mockBotTypeConfig.getPublishConfiguration()).thenReturn(publishConfig);
    when(publishConfig.getDefaultKey()).thenReturn(null); // Key is missing

    // Act & Assert
    assertThatThrownBy(() -> bot.publishDefaultOrder("payload"))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("no publish section present");
  }

  @Test
  @DisplayName("publishOrder should use StringPayloadSerializer for String payloads")
  void publishOrder_stringPayload_usesStringSerializer() {
    // Act
    bot.publishOrder("key", "action", "plain string");

    // Assert
    verify(mockBoticaClient, times(1)).publishOrder(eq("key"), eq("action"), eq("plain string"));
  }

  @Test
  @DisplayName("publishOrder should use JsonObjectPayloadSerializer for JSONObject payloads")
  void publishOrder_jsonObjectPayload_usesJsonObjectSerializer() {
    // Arrange
    JSONObject payload = new JSONObject();
    payload.put("test", "value");

    // Act
    bot.publishOrder("key", "action", payload);

    // Assert
    verify(mockBoticaClient, times(1))
        .publishOrder(eq("key"), eq("action"), eq("{\"test\":\"value\"}"));
  }

  @Test
  @DisplayName("publishOrder should use JacksonPayloadSerializer for POJO payloads")
  void publishOrder_pojoPayload_usesJacksonSerializer() {
    // Arrange
    class MyPojo {
      public String name = "test";
    }
    MyPojo payload = new MyPojo();

    // Act
    bot.publishOrder("key", "action", payload);

    // Assert
    verify(mockBoticaClient, times(1))
        .publishOrder(eq("key"), eq("action"), eq("{\"name\":\"test\"}"));
  }

  @Test
  @DisplayName("publishOrder should throw IllegalArgumentException for null payload")
  void publishOrder_nullPayload_throwsException() {
    // Act & Assert
    assertThatThrownBy(() -> bot.publishOrder("key", "action", null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Payload cannot be null");
  }

  @Test
  @DisplayName("publishOrder should throw IllegalStateException if no serializer is found")
  void publishOrder_noSerializerFound_throwsException() {
    // Arrange
    class Unserializable {}

    // Act & Assert
    assertThatThrownBy(() -> bot.publishOrder("key", "action", new Unserializable()))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("No payload serializer found for object of type");
  }
}
