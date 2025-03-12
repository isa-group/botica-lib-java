package es.us.isa.botica.bot;

import es.us.isa.botica.configuration.MainConfiguration;
import es.us.isa.botica.configuration.bot.BotInstanceConfiguration;
import es.us.isa.botica.configuration.bot.BotTypeConfiguration;
import es.us.isa.botica.configuration.broker.BrokerConfiguration;
import es.us.isa.botica.configuration.broker.RabbitMqConfiguration;
import es.us.isa.botica.protocol.BoticaClient;
import es.us.isa.botica.protocol.JacksonPacketConverter;
import es.us.isa.botica.protocol.RabbitMqBoticaClient;
import es.us.isa.botica.reflect.ComponentInspector;
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
  private static final File CONFIG_FILE = new File("/run/secrets/botica-config");

  private BotLauncher() {}

  /**
   * Loads the configuration for the bot instance running in the current container and starts the
   * given bot.
   *
   * @param userBot the bot instance to configure and execute
   * @param args the application arguments, typically passed from the {@code main} method
   */
  public static void run(BaseBot userBot, String[] args) {
    MainConfiguration configuration = loadConfiguration();
    String botType = System.getenv("BOTICA_BOT_TYPE");
    String botId = System.getenv("BOTICA_BOT_ID");

    BotTypeConfiguration typeConfiguration = configuration.getBotTypes().get(botType);
    BotInstanceConfiguration botConfiguration = typeConfiguration.getInstances().get(botId);
    BoticaClient boticaClient = buildClient(configuration, typeConfiguration, botConfiguration);

    Bot coreBot = new Bot(boticaClient, typeConfiguration, botConfiguration);
    userBot.setBot(coreBot);
    userBot.configure();

    ComponentInspector.registerHandlerMethods(coreBot, userBot.getClass(), userBot);

    try {
      coreBot.start();
    } catch (TimeoutException e) {
      throw new RuntimeException(e);
    }
    userBot.onStart();
  }

  private static MainConfiguration loadConfiguration() {
    if (!CONFIG_FILE.isFile()) {
      throw new IllegalStateException(
          "Couldn't find the needed configuration file. Are you manually starting this bot? Bots "
              + "should be started inside a container conveniently created by the botica director!");
    }
    ConfigurationFileLoader configurationFileLoader = new JacksonConfigurationFileLoader();
    return configurationFileLoader.load(CONFIG_FILE, MainConfiguration.class);
  }

  private static BoticaClient buildClient(
      MainConfiguration mainConfiguration,
      BotTypeConfiguration typeConfiguration,
      BotInstanceConfiguration botConfiguration) {
    BrokerConfiguration brokerConfiguration = mainConfiguration.getBrokerConfiguration();
    if (brokerConfiguration instanceof RabbitMqConfiguration) {
      return new RabbitMqBoticaClient(
          mainConfiguration, typeConfiguration, botConfiguration, new JacksonPacketConverter());
    }
    throw new UnsupportedOperationException("Unsupported broker type");
  }
}
