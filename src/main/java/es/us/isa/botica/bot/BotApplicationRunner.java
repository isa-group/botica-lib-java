package es.us.isa.botica.bot;

import es.us.isa.botica.configuration.MainConfiguration;
import es.us.isa.botica.util.configuration.ConfigurationFileLoader;
import es.us.isa.botica.util.configuration.JacksonConfigurationFileLoader;
import java.io.File;
import java.util.concurrent.TimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class that can be used to bootstrap and launch a {@link AbstractBotApplication} from a Java main
 * method.
 *
 * @author Alberto Mimbrero
 */
public final class BotApplicationRunner {
  private static final File CONFIG_FILE = new File("/run/secrets/botica-config");
  private static final Logger log = LoggerFactory.getLogger(BotApplicationRunner.class);

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

    Bot bot =
        new Bot(
            configuration.getBrokerConfiguration(),
            configuration.getBotTypes().get(botType),
            botId);
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
          "Couldn't find the needed configuration file. Are you manually starting the bot? The bot "
              + "should be started inside a container conveniently created by the botica director!");
    }
    ConfigurationFileLoader configurationFileLoader = new JacksonConfigurationFileLoader();
    return configurationFileLoader.load(file, MainConfiguration.class);
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
