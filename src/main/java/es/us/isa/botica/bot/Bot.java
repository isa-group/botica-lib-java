package es.us.isa.botica.bot;

import es.us.isa.botica.BoticaConstants;
import es.us.isa.botica.bot.payload.PayloadSerializer;
import es.us.isa.botica.bot.payload.support.JacksonPayloadSerializer;
import es.us.isa.botica.bot.payload.support.JsonObjectPayloadSerializer;
import es.us.isa.botica.bot.payload.support.StringPayloadSerializer;
import es.us.isa.botica.bot.shutdown.ShutdownHandler;
import es.us.isa.botica.configuration.bot.BotInstanceConfiguration;
import es.us.isa.botica.configuration.bot.BotPublishConfiguration;
import es.us.isa.botica.configuration.bot.lifecycle.BotLifecycleConfiguration;
import es.us.isa.botica.configuration.bot.lifecycle.BotLifecycleType;
import es.us.isa.botica.configuration.bot.lifecycle.ProactiveBotLifecycleConfiguration;
import es.us.isa.botica.configuration.bot.lifecycle.ReactiveBotLifecycleConfiguration;
import es.us.isa.botica.protocol.BoticaClient;
import es.us.isa.botica.protocol.HeartbeatPacket;
import es.us.isa.botica.protocol.OrderListener;
import es.us.isa.botica.protocol.client.ReadyPacket;
import java.io.File;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A bot instance in a botica environment.
 *
 * <p>{@link #start() Starting} the bot does not block any thread. The necessary threads are started
 * internally for broker message listeners and other tasks.
 *
 * <p><b>NOTE:</b> it is recommended to extend the {@link BaseBot} helper class and use its
 * convenience methods in order to implement bots. For a more advanced use, the bot object can be
 * accessed with {@link BaseBot#getBot()}.
 *
 * @author Alberto Mimbrero
 */
public class Bot {
  private static final File SHARED_DIRECTORY = new File("/shared");

  private final Logger log;
  private final BoticaClient boticaClient;
  private final BotInstanceConfiguration configuration;
  private final ShutdownHandler shutdownHandler;

  private final Map<Type, PayloadSerializer<?>> payloadSerializers = new HashMap<>();

  private boolean running = false;
  private Runnable proactiveTask;

  public Bot(BoticaClient boticaClient, BotInstanceConfiguration configuration) {
    this.boticaClient = boticaClient;
    this.configuration = configuration;
    this.shutdownHandler = new ShutdownHandler(this.boticaClient);

    this.registerPayloadSerializer(new StringPayloadSerializer());
    this.registerPayloadSerializer(new JsonObjectPayloadSerializer());
    this.registerPayloadSerializer(new JacksonPayloadSerializer());

    this.log = LoggerFactory.getLogger("Bot - " + configuration.getId());
  }

  /**
   * Sets the proactive task for this bot.
   *
   * @param task the task to set
   * @throws IllegalStateException if the bot lifecycle type is not {@code proactive}
   */
  public void setProactiveTask(Runnable task) {
    if (configuration.getLifecycleConfiguration().getType() != BotLifecycleType.PROACTIVE) {
      throw new IllegalStateException(
          "Cannot register a proactive task because this bot is not configured as proactive.");
    }
    this.proactiveTask = task;
  }

  /** Returns whether the proactive task is set. */
  public boolean isProactiveTaskSet() {
    return this.proactiveTask != null;
  }

  /**
   * Registers the given listener for the order this bot serves. The order is taken from the main
   * configuration file.
   *
   * @param orderListener the listener to register
   * @throws IllegalStateException if no default order is specified for this bot in the current
   *     botica environment configuration
   */
  public void registerOrderListener(OrderListener orderListener) {
    BotLifecycleConfiguration lifecycleConfiguration =
        this.configuration.getLifecycleConfiguration();
    String order =
        lifecycleConfiguration instanceof ReactiveBotLifecycleConfiguration
            ? ((ReactiveBotLifecycleConfiguration) lifecycleConfiguration).getOrder()
            : null;

    if (order == null) {
      throw new IllegalStateException(
          "No default order specified for this bot in the environment configuration file");
    }
    this.registerOrderListener(order, orderListener);
  }

  /**
   * Registers the given listener for the provided order.
   *
   * @param order the order to listen to
   * @param orderListener the listener to register
   */
  public void registerOrderListener(String order, OrderListener orderListener) {
    this.boticaClient.registerOrderListener(Objects.requireNonNull(order), orderListener);
  }

  /**
   * Publishes an order with the given payload. The key and order are taken from the main
   * configuration file.
   *
   * @param payload the payload of the order
   * @throws IllegalStateException if the bot type configuration does not specify a publish section
   */
  public void publishOrder(Object payload) {
    BotPublishConfiguration publishConfiguration =
        this.configuration.getTypeConfiguration().getPublishConfiguration();
    String key = publishConfiguration.getKey();
    String order = publishConfiguration.getOrder();
    if (key == null || key.isBlank() || order == null || order.isBlank()) {
      throw new IllegalStateException(
          "Cannot publish order: no publish section present in the bot type configuration.");
    }
    this.publishOrder(key, order, payload);
  }

  /**
   * Publishes an order.
   *
   * @param key the key to publish the order with
   * @param order the order to publish
   * @param message the message of the order
   */
  public void publishOrder(String key, String order, Object message) {
    String serializedPayload = serializePayload(message);
    this.boticaClient.publishOrder(
        Objects.requireNonNull(key),
        Objects.requireNonNull(order),
        Objects.requireNonNull(serializedPayload));
  }

  @SuppressWarnings("unchecked")
  private String serializePayload(Object payload) {
    if (payload == null) {
      throw new IllegalArgumentException("Payload cannot be null");
    }

    PayloadSerializer<?> serializer = payloadSerializers.get(payload.getClass());
    if (serializer != null) {
      return ((PayloadSerializer<Object>) serializer).serialize(payload);
    }

    for (PayloadSerializer<?> registeredSerializer : payloadSerializers.values()) {
      if (registeredSerializer.canSerialize(payload)) {
        return ((PayloadSerializer<Object>) registeredSerializer).serialize(payload);
      }
    }

    throw new IllegalStateException(
        String.format(
            "No payload serializer found for object of type %s", payload.getClass().getName()));
  }

  /** Returns the hostname of this bot's container. */
  public String getHostname() {
    return this.getBotHostname(this.configuration.getId());
  }

  /**
   * Returns the hostname of the given bot's container.
   *
   * @param botId the ID of the bot instance
   * @return the bot container's hostname
   */
  public String getBotHostname(String botId) {
    return BoticaConstants.CONTAINER_PREFIX + botId;
  }

  /** Returns the shared directory for all bots provided by Botica. */
  public File getSharedDirectory() {
    return SHARED_DIRECTORY;
  }

  /** Returns the shutdown handler of this bot */
  public ShutdownHandler getShutdownHandler() {
    return shutdownHandler;
  }

  /** Returns the configuration of this bot instance */
  public BotInstanceConfiguration getConfiguration() {
    return configuration;
  }

  public void registerPayloadSerializer(PayloadSerializer<?> serializer) {
    serializer
        .getSupportedTypes()
        .forEach(type -> this.registerPayloadSerializer(type, serializer));
  }

  public <T> void registerPayloadSerializer(
      Class<? super T> type, PayloadSerializer<T> serializer) {
    this.registerPayloadSerializer((Type) type, serializer);
  }

  public void registerPayloadSerializer(Type type, PayloadSerializer<?> serializer) {
    this.payloadSerializers.put(type, serializer);
  }

  /**
   * Starts the bot.
   *
   * @throws TimeoutException if the connection with the message broker cannot be established
   */
  public void start() throws TimeoutException {
    log.info("Establishing connection with the message broker...");
    this.boticaClient.connect();
    this.running = true;
    log.info("Connected to the message broker.");

    if (this.configuration.getLifecycleConfiguration().getType() == BotLifecycleType.PROACTIVE) {
      this.startProactiveScheduler();
    }
    this.setupHeartbeat();
    this.boticaClient.sendPacket(new ReadyPacket());
    log.info("Bot started.");
  }

  private void setupHeartbeat() {
    this.boticaClient.registerPacketListener(
        HeartbeatPacket.class, packet -> this.boticaClient.sendPacket(new HeartbeatPacket()));
  }

  private void startProactiveScheduler() {
    if (this.proactiveTask == null) {
      throw new IllegalStateException(
          "This bot is configured as a proactive bot, but no proactive task has been registered.");
    }
    ProactiveBotLifecycleConfiguration lifecycleConfiguration =
        (ProactiveBotLifecycleConfiguration) this.configuration.getLifecycleConfiguration();

    ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();
    if (lifecycleConfiguration.getPeriod() > 0) {
      this.scheduleRepeatingTask(lifecycleConfiguration, executorService);
    } else {
      this.scheduleTask(executorService, lifecycleConfiguration);
    }
  }

  private void scheduleTask(
      ScheduledExecutorService executorService,
      ProactiveBotLifecycleConfiguration lifecycleConfiguration) {
    executorService.schedule(
        () -> {
          if (isRunning()) {
            this.runProactiveTask();
            this.stop();
          }
          executorService.shutdown();
        },
        lifecycleConfiguration.getInitialDelay(),
        TimeUnit.SECONDS);
  }

  private void scheduleRepeatingTask(
      ProactiveBotLifecycleConfiguration lifecycleConfiguration,
      ScheduledExecutorService executorService) {
    executorService.scheduleWithFixedDelay(
        () -> {
          if (!isRunning()) {
            executorService.shutdownNow();
            return;
          }
          this.runProactiveTask();
        },
        lifecycleConfiguration.getInitialDelay(),
        lifecycleConfiguration.getPeriod(),
        TimeUnit.SECONDS);
  }

  private void runProactiveTask() {
    try {
      this.proactiveTask.run();
    } catch (Exception e) {
      log.error("an exception was risen during the bot proactive task", e);
    }
  }

  /** Returns whether the bot is running. */
  public boolean isRunning() {
    return this.running;
  }

  /** Closes the connection and stops the bot. */
  public void stop() {
    if (!this.running) {
      throw new IllegalStateException("bot is not running");
    }
    log.info("Closing connection with the message broker...");
    this.boticaClient.close();
    this.running = false;
    log.info("Bot stopped.");
  }
}
