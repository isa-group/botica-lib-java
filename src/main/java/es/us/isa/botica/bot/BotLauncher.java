package es.us.isa.botica.bot;

import es.us.isa.botica.configuration.MainConfiguration;
import es.us.isa.botica.configuration.bot.BotInstanceConfiguration;
import es.us.isa.botica.configuration.bot.BotTypeConfiguration;
import es.us.isa.botica.configuration.broker.BrokerConfiguration;
import es.us.isa.botica.configuration.broker.RabbitMqConfiguration;
import es.us.isa.botica.protocol.BoticaClient;
import es.us.isa.botica.protocol.JacksonPacketConverter;
import es.us.isa.botica.protocol.RabbitMqBoticaClient;
import es.us.isa.botica.util.annotation.VisibleForTesting;
import es.us.isa.botica.util.configuration.ConfigurationFileLoader;
import es.us.isa.botica.util.configuration.jackson.JacksonConfigurationFileLoader;
import java.io.File;
import java.util.concurrent.TimeoutException;

/**
 * Class that can be used to bootstrap and launch a {@link BaseBot} from a Java main method.
 *
 * @author Alberto Mimbrero
 */
public final class BotLauncher {
  @VisibleForTesting static File CONFIG_FILE = new File("/run/secrets/botica-config");

  private BotLauncher() {}

  /**
   * Loads the configuration for the bot running in the current container and starts the given bot.
   *
   * @param userBot the bot instance to configure and execute
   */
  public static void run(BaseBot userBot) {
    run(userBot, null);
  }

  /**
   * Loads the configuration for the bot running in the current container and starts the given bot.
   *
   * @param userBot the bot instance to configure and execute
   * @param args the application arguments, typically passed from the {@code main} method
   */
  public static void run(BaseBot userBot, String[] args) {
    String botType = System.getProperty("BOTICA_BOT_TYPE");
    String botId = System.getProperty("BOTICA_BOT_ID");

    if (botType == null || botId == null) throwNoBoticaEnvironmentException();

    MainConfiguration configuration = loadConfiguration();
    BotInstanceConfiguration botConfiguration = getBotConfiguration(configuration, botType, botId);
    BoticaClient boticaClient = buildClient(configuration, botConfiguration);

    Bot coreBot = new Bot(boticaClient, botConfiguration);
    userBot.setBot(coreBot);
    userBot.configure();

    userBot.getComponentInspector().registerHandlerMethods(coreBot, userBot.getClass(), userBot);

    try {
      coreBot.start();
    } catch (TimeoutException e) {
      throw new RuntimeException(e);
    }
    userBot.onStart();
  }

  private static MainConfiguration loadConfiguration() {
    if (!CONFIG_FILE.isFile()) throwNoBoticaEnvironmentException();

    ConfigurationFileLoader configurationFileLoader = new JacksonConfigurationFileLoader();
    return configurationFileLoader.load(CONFIG_FILE, MainConfiguration.class);
  }

  private static BotInstanceConfiguration getBotConfiguration(
      MainConfiguration configuration, String botType, String botId) {
    BotTypeConfiguration typeConfiguration = configuration.getBotTypes().get(botType);
    BotInstanceConfiguration botConfiguration = typeConfiguration.getDeclaredInstances().get(botId);

    if (botConfiguration == null) { // it's a generic replica
      botConfiguration = new BotInstanceConfiguration();
      botConfiguration.setTypeConfiguration(typeConfiguration);
      botConfiguration.setId(botId);
    }
    return botConfiguration;
  }

  private static BoticaClient buildClient(
      MainConfiguration mainConfiguration, BotInstanceConfiguration botConfiguration) {
    BrokerConfiguration brokerConfiguration = mainConfiguration.getBrokerConfiguration();
    if (brokerConfiguration instanceof RabbitMqConfiguration) {
      return new RabbitMqBoticaClient(
          mainConfiguration, botConfiguration, new JacksonPacketConverter());
    }
    throw new UnsupportedOperationException("Unsupported broker type");
  }

  private static void throwNoBoticaEnvironmentException() {
    throw new IllegalStateException(
        "Not running inside a Botica environment. Are you manually starting this bot? Bots "
            + "should be started inside a container conveniently created by the botica director!");
  }
}
