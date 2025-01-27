package es.us.isa.botica.protocol;

import es.us.isa.botica.protocol.query.RequestPacket;
import es.us.isa.botica.protocol.query.ResponsePacket;

@FunctionalInterface
public interface RequestPacketListener<
    RequestPacketT extends RequestPacket<ResponsePacketT>, ResponsePacketT extends ResponsePacket> {
  ResponsePacketT onPacketRequestReceived(RequestPacketT packet);
}
