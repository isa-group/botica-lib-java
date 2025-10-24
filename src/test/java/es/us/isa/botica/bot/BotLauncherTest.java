package es.us.isa.botica.bot;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentCaptor.captor;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import es.us.isa.botica.configuration.MainConfiguration;
import es.us.isa.botica.configuration.bot.BotInstanceConfiguration;
import es.us.isa.botica.configuration.bot.BotTypeConfiguration;
import es.us.isa.botica.configuration.broker.BrokerConfiguration;
import es.us.isa.botica.configuration.broker.RabbitMqConfiguration;
import es.us.isa.botica.inspect.ComponentInspector;
import es.us.isa.botica.protocol.BoticaClient;
import es.us.isa.botica.protocol.RabbitMqBoticaClient;
import es.us.isa.botica.util.configuration.jackson.JacksonConfigurationFileLoader;
import es.us.isa.botica.util.configuration.validate.ValidationReport;
import java.io.File;
import java.util.Map;
import java.util.concurrent.TimeoutException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoSettings;

@MockitoSettings
class BotLauncherTest {
  private static final String BOT_TYPE = "test-type";
  private static final String BOT_ID = "test-bot-1";

  @Spy private BaseBot userBot;
  @Mock private ComponentInspector componentInspector;
  @Mock private BoticaClient boticaClient;

  private File originalConfigFile;

  @BeforeEach
  void setUp() {
    originalConfigFile = BotLauncher.CONFIG_FILE;
    System.setProperty("BOTICA_BOT_TYPE", BOT_TYPE);
    System.setProperty("BOTICA_BOT_ID", BOT_ID);
  }

  @AfterEach
  void tearDown() {
    System.clearProperty("BOTICA_BOT_TYPE");
    System.clearProperty("BOTICA_BOT_ID");
    BotLauncher.CONFIG_FILE = originalConfigFile;
  }

  @Test
  @DisplayName("run() should correctly initialize, configure, and start the bot")
  void run_shouldConfigureAndStartBotCorrectly() throws TimeoutException {
    // Arrange
    MainConfiguration mainConfig = new MainConfiguration();
    BotTypeConfiguration botTypeConfig = new BotTypeConfiguration();
    BotInstanceConfiguration botInstanceConfig = new BotInstanceConfiguration();
    botInstanceConfig.setTypeConfiguration(botTypeConfig);
    mainConfig.setBrokerConfiguration(new RabbitMqConfiguration());
    botTypeConfig.setDeclaredInstances(Map.of(BOT_ID, botInstanceConfig));
    mainConfig.setBotTypes(Map.of(BOT_TYPE, botTypeConfig));

    File mockConfigFile = mock(File.class);
    when(mockConfigFile.isFile()).thenReturn(true);
    BotLauncher.CONFIG_FILE = mockConfigFile;

    ArgumentCaptor<Bot> botCaptor = captor();
    when(userBot.getComponentInspector()).thenReturn(componentInspector);

    try (MockedConstruction<JacksonConfigurationFileLoader> loaderConstruction =
            mockConstruction(
                JacksonConfigurationFileLoader.class,
                (mock, context) -> {
                  when(mock.load(BotLauncher.CONFIG_FILE, MainConfiguration.class))
                      .thenReturn(mainConfig);
                });
        MockedConstruction<RabbitMqBoticaClient> clientConstruction =
            mockConstruction(
                RabbitMqBoticaClient.class,
                (mock, context) -> {
                  boticaClient = mock;
                })) {
      // Act
      BotLauncher.run(userBot, null);

      // Assert
      InOrder inOrder = inOrder(userBot, componentInspector, boticaClient);

      // 1. The core Bot instance is set
      inOrder.verify(userBot).setBot(botCaptor.capture());
      Bot capturedCoreBot = botCaptor.getValue();
      assertNotNull(capturedCoreBot);
      assertEquals(BOT_ID, capturedCoreBot.getConfiguration().getId());
      assertEquals(botTypeConfig, capturedCoreBot.getConfiguration().getTypeConfiguration());

      // 2. User's configure() method is called
      inOrder.verify(userBot).configure();

      // 3. Handlers are registered
      inOrder
          .verify(componentInspector)
          .registerHandlerMethods(eq(capturedCoreBot), eq(userBot.getClass()), eq(userBot));

      // 4. The core bot's start() is called, which triggers the client
      // connection
      inOrder.verify(boticaClient).connect();

      // 5. User's onStart() is called last
      verify(userBot).onStart();
    }
  }

  @Test
  @DisplayName("run() should throw IllegalStateException if config file is missing")
  void run_whenConfigFileIsMissing_shouldThrowIllegalStateException() {
    // Arrange
    File mockFile = mock(File.class);
    when(mockFile.isFile()).thenReturn(false);
    BotLauncher.CONFIG_FILE = mockFile;

    // Act & Assert
    assertThatThrownBy(() -> BotLauncher.run(userBot, null))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("Not running inside a Botica environment");
  }

  @Test
  @DisplayName("run() should throw IllegalStateException if BOTICA_BOT_TYPE property is not set")
  void run_whenBotIdPropertyMissing_shouldThrowException() {
    // Arrange
    System.clearProperty("BOTICA_BOT_TYPE");

    // Act & Assert
    assertThatThrownBy(() -> BotLauncher.run(userBot, null))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("Not running inside a Botica environment");
  }

  @Test
  @DisplayName("run() should throw IllegalStateException if BOTICA_BOT_ID property is not set")
  void run_whenBotTypePropertyMissing_shouldThrowException() {
    // Arrange
    System.clearProperty("BOTICA_BOT_ID");

    // Act & Assert
    assertThatThrownBy(() -> BotLauncher.run(userBot, null))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("Not running inside a Botica environment");
  }

  // Custom unsupported broker for testing
  static class AlienMqConfiguration implements BrokerConfiguration {
    @Override
    public void validate(ValidationReport report) {}
  }

  @Test
  @DisplayName("run() should throw exception if broker type is unsupported")
  void run_whenBrokerIsUnsupported_shouldThrowException() {
    // Arrange
    MainConfiguration mainConfig = new MainConfiguration();
    mainConfig.setBrokerConfiguration(new AlienMqConfiguration());
    mainConfig.setBotTypes(Map.of(BOT_TYPE, new BotTypeConfiguration()));

    File mockFile = mock(File.class);
    when(mockFile.isFile()).thenReturn(true);
    BotLauncher.CONFIG_FILE = mockFile;

    try (MockedConstruction<JacksonConfigurationFileLoader> loaderConstruction =
        mockConstruction(
            JacksonConfigurationFileLoader.class,
            (mock, context) -> when(mock.load(any(), any())).thenReturn(mainConfig))) {
      // Act & Assert
      UnsupportedOperationException ex =
          assertThrows(UnsupportedOperationException.class, () -> BotLauncher.run(userBot, null));
      assertEquals("Unsupported broker type", ex.getMessage());
      verify(userBot, never()).onStart();
    }
  }
}
