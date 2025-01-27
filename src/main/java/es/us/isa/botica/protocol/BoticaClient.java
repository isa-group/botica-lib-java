package es.us.isa.botica.protocol;

import es.us.isa.botica.protocol.query.RequestPacket;
import es.us.isa.botica.protocol.query.ResponsePacket;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public interface BoticaClient {
  void connect() throws TimeoutException;

  boolean isConnected();

  void registerOrderListener(String order, OrderListener listener);

  void publishOrder(String key, String order, String message);

  <P extends Packet> void registerPacketListener(Class<P> packetClass, PacketListener<P> listener);

  <RequestPacketT extends RequestPacket<ResponsePacketT>, ResponsePacketT extends ResponsePacket>
      void registerPacketListener(
          Class<RequestPacketT> packetClass,
          RequestPacketListener<RequestPacketT, ResponsePacketT> listener);

  void sendPacket(Packet packet);

  <ResponsePacketT extends ResponsePacket> void sendPacket(
      RequestPacket<ResponsePacketT> packet,
      String botId,
      PacketListener<ResponsePacketT> callback,
      Runnable timeoutCallback);

  <ResponsePacketT extends ResponsePacket> void sendPacket(
      RequestPacket<ResponsePacketT> packet,
      String botId,
      PacketListener<ResponsePacketT> callback,
      Runnable timeoutCallback,
      long timeout,
      TimeUnit timeoutUnit);

  void close();
}
