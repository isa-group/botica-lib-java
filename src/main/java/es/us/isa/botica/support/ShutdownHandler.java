package es.us.isa.botica.support;

import es.us.isa.botica.client.BoticaClient;
import es.us.isa.botica.protocol.ShutdownRequestPacket;

public class ShutdownHandler {
  private final BoticaClient boticaClient;

  public ShutdownHandler(BoticaClient boticaClient) {
    this.boticaClient = boticaClient;
    boticaClient.registerPacketListener(ShutdownRequestPacket.class, this::onShutdownRequest);
  }

  private void onShutdownRequest(ShutdownRequestPacket packet) {
    // TODO
  }
}
