package es.us.isa.botica.protocol;

import java.util.concurrent.TimeoutException;

public interface BoticaClient {
  void connect() throws TimeoutException;

  boolean isConnected();

  void registerOrderListener(String order, OrderListener listener);

  void publishOrder(String key, String order, String message);

  <P extends Packet> void registerPacketListener(Class<P> packetClass, PacketListener<P> listener);

  void sendPacket(Packet packet);

  void close();
}
