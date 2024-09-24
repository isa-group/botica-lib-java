package es.us.isa.botica.bot;

import es.us.isa.botica.protocol.BoticaClient;
import es.us.isa.botica.protocol.RabbitMqBoticaClient;
import es.us.isa.botica.configuration.MainConfiguration;
import es.us.isa.botica.configuration.bot.BotInstanceConfiguration;
import es.us.isa.botica.configuration.bot.BotTypeConfiguration;
import es.us.isa.botica.configuration.broker.BrokerConfiguration;
import es.us.isa.botica.configuration.broker.RabbitMqConfiguration;
import es.us.isa.botica.protocol.JacksonPacketConverter;
import es.us.isa.botica.util.configuration.ConfigurationFileLoader;
import es.us.isa.botica.util.configuration.JacksonConfigurationFileLoader;
import java.io.File;
import java.util.concurrent.TimeoutException;

/**
 * Class that can be used to bootstrap and launch a {@link AbstractBotApplication} from a Java main
 * method.
 *
 * @author Alberto Mimbrero
 */
public final class BotApplicationRunner {
  private static final File CONFIG_FILE = new File("/run/secrets/botica-config");

  private BotApplicationRunner() {}

  /**
   * Loads the configuration of the bot instance for the container that this application is running
   * on and runs the given bot application.
   *
   * @param botApplication the application to run
   * @param args the application arguments (usually passed from a Java main method)
   */
  public static void run(AbstractBotApplication botApplication, String[] args) {
    MainConfiguration configuration = loadConfiguration(CONFIG_FILE);
    String botType = System.getenv("BOTICA_BOT_TYPE");
    String botId = System.getenv("BOTICA_BOT_ID");

    BotTypeConfiguration typeConfiguration = configuration.getBotTypes().get(botType);
    BotInstanceConfiguration botConfiguration = typeConfiguration.getInstances().get(botId);
    BoticaClient boticaClient = buildClient(configuration, typeConfiguration, botConfiguration);

    Bot bot = new Bot(boticaClient, typeConfiguration, botConfiguration);
    botApplication.setBot(bot);
    botApplication.configure();
    registerAction(botApplication, bot);

    try {
      bot.start();
    } catch (TimeoutException e) {
      throw new RuntimeException(e);
    }
  }

  private static MainConfiguration loadConfiguration(File file) {
    if (!file.isFile()) {
      throw new IllegalStateException(
          "Couldn't find the needed configuration file. Are you manually starting this bot? Bots "
              + "should be started inside a container conveniently created by the botica director!");
    }
    ConfigurationFileLoader configurationFileLoader = new JacksonConfigurationFileLoader();
    return configurationFileLoader.load(file, MainConfiguration.class);
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

  private static void registerAction(AbstractBotApplication botApplication, Bot bot) {
    switch (bot.getTypeConfiguration().getLifecycleConfiguration().getType()) {
      case PROACTIVE:
        if (!bot.isProactiveActionSet()) { // action can also be registered in #configure()
          bot.setProactiveAction(botApplication::executeAction);
        }
        break;
      case REACTIVE:
        bot.registerOrderListener((order, message) -> botApplication.onOrderReceived(message));
        break;
      default:
        throw new UnsupportedOperationException("unsupported lifecycle type");
    }
  }
}
