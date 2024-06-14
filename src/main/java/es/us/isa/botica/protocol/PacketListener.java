package es.us.isa.botica.protocol;

@FunctionalInterface
public interface PacketListener<P extends Packet> {
  void onPacketReceived(P packet);
}
