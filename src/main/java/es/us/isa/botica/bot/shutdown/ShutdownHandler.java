package es.us.isa.botica.bot.shutdown;

import es.us.isa.botica.protocol.BoticaClient;
import es.us.isa.botica.protocol.client.ShutdownResponsePacket;
import es.us.isa.botica.protocol.server.ShutdownRequestPacket;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ShutdownHandler {
  private static final Logger log = LoggerFactory.getLogger(ShutdownHandler.class);

  private final List<ShutdownRequestHook> shutdownRequestHooks = new ArrayList<>();

  public ShutdownHandler(BoticaClient boticaClient) {
    boticaClient.registerPacketListener(ShutdownRequestPacket.class, this::onShutdownRequest);
  }

  /**
   * Registers the given {@link ShutdownRequestHook}.
   *
   * @param hook the shutdown hook to register
   */
  public void registerShutdownRequestHook(ShutdownRequestHook hook) {
    this.shutdownRequestHooks.add(hook);
  }

  /**
   * Unregisters the given {@link ShutdownRequestHook}.
   *
   * @param hook the shutdown hook to unregister
   */
  public void unregisterShutdownRequestHook(ShutdownRequestHook hook) {
    this.shutdownRequestHooks.remove(hook);
  }

  private ShutdownResponsePacket onShutdownRequest(ShutdownRequestPacket packet) {
    ShutdownRequest request = new ShutdownRequest(packet.isForced());
    ShutdownResponse response = new ShutdownResponse();
    for (ShutdownRequestHook hook : this.shutdownRequestHooks) {
      try {
        hook.onShutdownRequest(request, response);
      } catch (Exception e) {
        log.error("An exception occurred while executing a shutdown hook.", e);
      }
    }
    return new ShutdownResponsePacket(!response.isCanceled());
  }
}
