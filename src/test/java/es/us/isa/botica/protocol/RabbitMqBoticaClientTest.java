package es.us.isa.botica.protocol;

import static es.us.isa.botica.rabbitmq.RabbitMqConstants.BOT_ORDERS_FORMAT;
import static es.us.isa.botica.rabbitmq.RabbitMqConstants.BOT_PROTOCOL_IN_FORMAT;
import static es.us.isa.botica.rabbitmq.RabbitMqConstants.BOT_TYPE_ORDERS_BROADCAST_FORMAT;
import static es.us.isa.botica.rabbitmq.RabbitMqConstants.BOT_TYPE_ORDERS_DISTRIBUTED_FORMAT;
import static es.us.isa.botica.rabbitmq.RabbitMqConstants.CONTAINER_NAME;
import static es.us.isa.botica.rabbitmq.RabbitMqConstants.DIRECTOR_PROTOCOL;
import static es.us.isa.botica.rabbitmq.RabbitMqConstants.ORDER_EXCHANGE;
import static es.us.isa.botica.rabbitmq.RabbitMqConstants.PROTOCOL_EXCHANGE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import es.us.isa.botica.configuration.MainConfiguration;
import es.us.isa.botica.configuration.bot.BotInstanceConfiguration;
import es.us.isa.botica.configuration.bot.BotSubscribeConfiguration;
import es.us.isa.botica.configuration.bot.BotSubscribeConfiguration.RoutingStrategy;
import es.us.isa.botica.configuration.bot.BotTypeConfiguration;
import es.us.isa.botica.configuration.broker.RabbitMqConfiguration;
import es.us.isa.botica.protocol.client.BotPacket;
import es.us.isa.botica.protocol.client.ReadyPacket;
import es.us.isa.botica.protocol.query.QueryHandler;
import es.us.isa.botica.protocol.server.ShutdownRequestPacket;
import es.us.isa.botica.rabbitmq.RabbitMqClient;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoSettings;

@MockitoSettings
class RabbitMqBoticaClientTest {
  private static final String BOT_ID = "test-bot-42";
  private static final String BOT_TYPE_ID = "analyzer";

  @Mock private RabbitMqClient rabbitClient;
  @Mock private PacketConverter packetConverter;
  @Mock private QueryHandler queryHandler;
  @Mock private OrderListener orderListener;

  @Captor private ArgumentCaptor<Consumer<String>> messageConsumerCaptor;
  @Captor private ArgumentCaptor<BotPacket> botPacketCaptor;

  private RabbitMqBoticaClient boticaClient;
  private BotTypeConfiguration botTypeConfig;

  @BeforeEach
  void setUp() {
    MainConfiguration mainConfig = new MainConfiguration();
    RabbitMqConfiguration rabbitConfig = new RabbitMqConfiguration();
    rabbitConfig.setUsername("user");
    rabbitConfig.setPassword("pass");
    mainConfig.setBrokerConfiguration(rabbitConfig);

    BotInstanceConfiguration botConfig = new BotInstanceConfiguration();
    botConfig.setId(BOT_ID);

    botTypeConfig = new BotTypeConfiguration();
    botTypeConfig.setId(BOT_TYPE_ID);
    botConfig.setTypeConfiguration(botTypeConfig);

    boticaClient =
        new RabbitMqBoticaClient(
            mainConfig, botConfig, packetConverter, rabbitClient, queryHandler);
  }

  @Test
  @DisplayName("connect() should establish connection and set up protocol queue")
  void connect_shouldEstablishConnectionAndInstallProtocolQueue() throws Exception {
    // Arrange
    botTypeConfig.setSubscribeConfigurations(List.of()); // No order listeners
    String protocolInQueue = String.format(BOT_PROTOCOL_IN_FORMAT, BOT_ID);

    // Act
    boticaClient.connect();

    // Assert
    InOrder inOrder = inOrder(rabbitClient);
    inOrder
        .verify(rabbitClient)
        .connect(
            "user", "pass", "botica-" + CONTAINER_NAME // Verifies container name build
            );
    inOrder.verify(rabbitClient).createQueue(protocolInQueue);
    inOrder.verify(rabbitClient).bind(PROTOCOL_EXCHANGE, protocolInQueue, protocolInQueue);
    inOrder.verify(rabbitClient).subscribe(eq(protocolInQueue), any());
  }

  @Test
  @DisplayName("connect() should set up distributed, broadcast, and individual order queues")
  void connect_shouldSetUpAllOrderQueues() throws Exception {
    // Arrange
    BotSubscribeConfiguration distSub = new BotSubscribeConfiguration();
    distSub.setStrategy(RoutingStrategy.DISTRIBUTED);
    BotSubscribeConfiguration broadSub = new BotSubscribeConfiguration();
    broadSub.setStrategy(RoutingStrategy.BROADCAST);
    botTypeConfig.setSubscribeConfigurations(List.of(distSub, broadSub));

    String distributedQueue = String.format(BOT_TYPE_ORDERS_DISTRIBUTED_FORMAT, BOT_TYPE_ID);
    String broadcastQueue = String.format(BOT_TYPE_ORDERS_BROADCAST_FORMAT, BOT_TYPE_ID);
    String ownQueue = String.format(BOT_ORDERS_FORMAT, BOT_ID);

    // Act
    boticaClient.connect();

    // Assert
    // Verify subscriptions to type-level queues
    verify(rabbitClient).subscribe(eq(distributedQueue), any());
    verify(rabbitClient).subscribe(eq(broadcastQueue), any());

    // Verify creation and subscription to the bot's own queue
    InOrder ownQueueOrder = inOrder(rabbitClient);
    ownQueueOrder.verify(rabbitClient).createQueue(ownQueue);
    ownQueueOrder.verify(rabbitClient).bind(ORDER_EXCHANGE, ownQueue, ownQueue);
    ownQueueOrder.verify(rabbitClient).subscribe(eq(ownQueue), any());
  }

  @Test
  @DisplayName("publishOrder() should publish a correctly formatted JSON message")
  void publishOrder_shouldPublishFormattedJson() {
    // Arrange
    String key = "routing.key";
    String action = "process-data";
    String payload = "{\"value\": 123}";
    ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);

    // Act
    boticaClient.publishOrder(key, action, payload);

    // Assert
    verify(rabbitClient).publish(eq(ORDER_EXCHANGE), eq(key), messageCaptor.capture());
    JSONObject sentJson = new JSONObject(messageCaptor.getValue());
    assertEquals(action, sentJson.getString("action"));
    assertEquals(payload, sentJson.getString("payload"));
  }

  @Test
  @DisplayName("sendPacket() should wrap packet in BotPacket and publish")
  void sendPacket_shouldWrapAndPublish() {
    // Arrange
    ReadyPacket readyPacket = new ReadyPacket();
    String serializedPacket = "{\"type\":\"ReadyPacket\"}";
    when(packetConverter.serialize(any(BotPacket.class))).thenReturn(serializedPacket);

    // Act
    boticaClient.sendPacket(readyPacket);

    // Assert
    verify(packetConverter).serialize(botPacketCaptor.capture());
    assertEquals(BOT_ID, botPacketCaptor.getValue().getBotId());
    assertEquals(readyPacket, botPacketCaptor.getValue().getPacket());

    verify(rabbitClient).publish(PROTOCOL_EXCHANGE, DIRECTOR_PROTOCOL, serializedPacket);
  }

  @Test
  @DisplayName("sendPacket() with callback should register a query with the QueryHandler")
  void sendPacket_withCallback_shouldRegisterQuery() {
    // Arrange
    ShutdownRequestPacket requestPacket = new ShutdownRequestPacket();

    // Act
    boticaClient.sendPacket(requestPacket, responsePacket -> {}, () -> {}, 5, TimeUnit.MINUTES);

    // Assert
    verify(queryHandler)
        .registerQuery(eq(requestPacket), any(), any(), eq(5L), eq(TimeUnit.MINUTES));
    // Also verify the packet is sent
    verify(rabbitClient).publish(any(), any(), any());
  }

  @Test
  @DisplayName("Received order with registered listener should trigger the listener")
  void onOrderReceived_shouldTriggerListener() throws Exception {
    // Arrange
    String action = "analyze";
    String payload = "some-data";
    String message = new JSONObject(Map.of("action", action, "payload", payload)).toString();

    boticaClient.registerOrderListener(action, orderListener);
    boticaClient.connect();

    String ordersQueue = String.format(BOT_ORDERS_FORMAT, BOT_ID);
    verify(rabbitClient).subscribe(eq(ordersQueue), messageConsumerCaptor.capture());
    Consumer<String> consumer = messageConsumerCaptor.getValue();

    // Act
    consumer.accept(message);

    // Assert
    verify(orderListener).onMessageReceived(action, payload);
  }

  @Test
  @DisplayName("Received order with no matching listener should be ignored")
  void onOrderReceived_shouldBeIgnoredIfNoListener() throws Exception {
    // Arrange
    String action = "analyze";
    String wrongAction = "process";
    String payload = "some-data";
    String message = new JSONObject(Map.of("action", wrongAction, "payload", payload)).toString();

    boticaClient.registerOrderListener(action, orderListener);
    boticaClient.connect();

    String ordersQueue = String.format(BOT_ORDERS_FORMAT, BOT_ID);
    verify(rabbitClient).subscribe(eq(ordersQueue), messageConsumerCaptor.capture());
    Consumer<String> consumer = messageConsumerCaptor.getValue();

    // Act
    consumer.accept(message);

    // Assert
    verify(orderListener, never()).onMessageReceived(any(), any());
  }

  @Test
  @DisplayName("close() should delegate to RabbitMqClient")
  void close_shouldDelegate() {
    // Act
    boticaClient.close();

    // Assert
    verify(rabbitClient).closeConnection();
  }
}
