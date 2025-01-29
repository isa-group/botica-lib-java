package es.us.isa.botica.protocol;

import static es.us.isa.botica.BoticaConstants.CONTAINER_PREFIX;
import static es.us.isa.botica.rabbitmq.RabbitMqConstants.*;

import es.us.isa.botica.configuration.MainConfiguration;
import es.us.isa.botica.configuration.bot.BotInstanceConfiguration;
import es.us.isa.botica.configuration.bot.BotSubscribeConfiguration;
import es.us.isa.botica.configuration.bot.BotSubscribeConfiguration.RoutingStrategy;
import es.us.isa.botica.configuration.bot.BotTypeConfiguration;
import es.us.isa.botica.configuration.broker.RabbitMqConfiguration;
import es.us.isa.botica.protocol.client.BotPacket;
import es.us.isa.botica.protocol.query.QueryHandler;
import es.us.isa.botica.protocol.query.RequestPacket;
import es.us.isa.botica.protocol.query.ResponsePacket;
import es.us.isa.botica.rabbitmq.RabbitMqClient;
import es.us.isa.botica.util.ExecutorUtils;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * RabbitMQ client implementation for the botica protocol.
 *
 * @author Alberto Mimbrero
 */
public class RabbitMqBoticaClient implements BoticaClient {
  private static final Logger log = LoggerFactory.getLogger(RabbitMqBoticaClient.class);

  private final MainConfiguration mainConfiguration;
  private final BotTypeConfiguration typeConfiguration;
  private final BotInstanceConfiguration botConfiguration;
  private final PacketConverter packetConverter;

  private final QueryHandler queryHandler;
  private final RabbitMqClient rabbitClient;
  private final Map<String, List<OrderListener>> orderListeners = new HashMap<>();
  private final Map<Class<?>, List<PacketListener<?>>> packetListeners = new HashMap<>();

  public RabbitMqBoticaClient(
      MainConfiguration mainConfiguration,
      BotTypeConfiguration typeConfiguration,
      BotInstanceConfiguration botConfiguration,
      PacketConverter packetConverter) {
    this.mainConfiguration = mainConfiguration;
    this.typeConfiguration = typeConfiguration;
    this.botConfiguration = botConfiguration;
    this.packetConverter = packetConverter;

    this.queryHandler = new QueryHandler(ExecutorUtils.newDaemonSingleThreadScheduledExecutor());
    this.rabbitClient = new RabbitMqClient();
  }

  @Override
  public void connect() throws TimeoutException {
    RabbitMqConfiguration configuration =
        (RabbitMqConfiguration) this.mainConfiguration.getBrokerConfiguration();

    this.rabbitClient.connect(
        configuration.getUsername(), configuration.getPassword(), this.buildContainerName());

    this.installProtocol();
    this.listenToOrders();
  }

  private void installProtocol() {
    String protocolIn = String.format(BOT_PROTOCOL_IN_FORMAT, botConfiguration.getId());
    this.rabbitClient.createQueue(protocolIn);
    this.rabbitClient.bind(PROTOCOL_EXCHANGE, protocolIn, protocolIn);
    this.rabbitClient.subscribe(protocolIn, this::callPacketListeners);
  }

  @SuppressWarnings("unchecked")
  private <P extends Packet> void callPacketListeners(String rawPacket) {
    Packet packet = this.packetConverter.deserialize(rawPacket);
    List<PacketListener<?>> listeners = this.packetListeners.get(packet.getClass());
    if (listeners == null) {
      return;
    }
    for (PacketListener<?> listener : listeners) {
      ((PacketListener<P>) listener).onPacketReceived((P) packet);
    }
  }

  private void listenToOrders() {
    Set<RoutingStrategy> strategies =
        typeConfiguration.getSubscribeConfigurations().stream()
            .map(BotSubscribeConfiguration::getStrategy)
            .collect(Collectors.toSet());

    if (strategies.contains(RoutingStrategy.DISTRIBUTED)) {
      String queue = String.format(BOT_TYPE_ORDERS_DISTRIBUTED_FORMAT, typeConfiguration.getId());
      this.listenToOrders(queue);
    }
    if (strategies.contains(RoutingStrategy.BROADCAST)) {
      String queue = String.format(BOT_TYPE_ORDERS_BROADCAST_FORMAT, typeConfiguration.getId());
      this.listenToOrders(queue);
    }
    this.listenToOwnQueue();
  }

  private void listenToOwnQueue() {
    String botQueue = String.format(BOT_ORDERS_FORMAT, botConfiguration.getId());
    this.rabbitClient.createQueue(botQueue);
    this.rabbitClient.bind(ORDER_EXCHANGE, botQueue, botQueue);
    this.listenToOrders(botQueue);
  }

  private void listenToOrders(String queue) {
    log.debug("Listening to {}", queue);
    this.rabbitClient.subscribe(
        queue,
        message -> {
          log.debug("Incoming message from queue {}: {}", queue, message);
          JSONObject root = new JSONObject(message);
          this.callOrderListeners(root.getString("order"), root.getString("message"));
        });
  }

  private void callOrderListeners(String order, String message) {
    List<OrderListener> listeners = this.orderListeners.get(order);
    if (listeners == null) return;
    for (OrderListener listener : listeners) {
      try {
        listener.onMessageReceived(order, message);
      } catch (Exception e) {
        log.error("An exception was thrown while consuming an order.", e);
      }
    }
  }

  @Override
  public boolean isConnected() {
    return this.rabbitClient.isConnected();
  }

  @Override
  public void registerOrderListener(String order, OrderListener listener) {
    log.debug("New order listener for {}", order);
    this.orderListeners.computeIfAbsent(order, k -> new ArrayList<>()).add(listener);
  }

  @Override
  public void publishOrder(String key, String order, String message) {
    String json = new JSONObject(Map.of("order", order, "message", message)).toString();
    log.debug("Publishing order with key {}: {}", key, json);
    this.rabbitClient.publish(ORDER_EXCHANGE, key, json);
  }

  @Override
  public <P extends Packet> void registerPacketListener(
      Class<P> packetClass, PacketListener<P> listener) {
    this.ensureResponsePacketListener(packetClass);
    this.packetListeners.get(packetClass).add(listener);
  }

  private <P extends Packet> void ensureResponsePacketListener(Class<P> packetClass) {
    List<PacketListener<?>> listeners =
        this.packetListeners.computeIfAbsent(packetClass, c -> new ArrayList<>());
    if (listeners.isEmpty() && ResponsePacket.class.isAssignableFrom(packetClass)) {
      listeners.add(response -> this.queryHandler.acceptResponse((ResponsePacket) response));
    }
  }

  @Override
  public <
          RequestPacketT extends RequestPacket<ResponsePacketT>,
          ResponsePacketT extends ResponsePacket>
      void registerPacketListener(
          Class<RequestPacketT> packetClass,
          RequestPacketListener<RequestPacketT, ResponsePacketT> listener) {
    this.registerPacketListener(
        packetClass,
        request -> {
          ResponsePacketT response = listener.onPacketRequestReceived(request);
          response.setRequestId(request.getRequestId());
          this.sendPacket(response);
        });
  }

  @Override
  public void sendPacket(Packet packet) {
    BotPacket wrapper = new BotPacket(this.botConfiguration.getId(), packet);
    String raw = this.packetConverter.serialize(wrapper);
    this.rabbitClient.publish(PROTOCOL_EXCHANGE, DIRECTOR_PROTOCOL, raw);
  }

  @Override
  public <ResponsePacketT extends ResponsePacket> void sendPacket(
      RequestPacket<ResponsePacketT> packet,
      PacketListener<ResponsePacketT> callback,
      Runnable timeoutCallback,
      long timeout,
      TimeUnit timeoutUnit) {
    this.ensureResponsePacketListener(packet.getResponsePacketClass());
    this.queryHandler.registerQuery(
        packet, callback::onPacketReceived, timeoutCallback, timeout, timeoutUnit);
    this.sendPacket(packet);
  }

  @Override
  public void close() {
    this.rabbitClient.closeConnection();
  }

  private String buildContainerName() {
    return CONTAINER_PREFIX + CONTAINER_NAME;
  }
}
