package es.us.isa.botica.bot.shutdown;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentCaptor.captor;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import es.us.isa.botica.protocol.BoticaClient;
import es.us.isa.botica.protocol.RequestPacketListener;
import es.us.isa.botica.protocol.client.ShutdownResponsePacket;
import es.us.isa.botica.protocol.server.ShutdownRequestPacket;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoSettings;

@MockitoSettings
@DisplayName("Bot-Side ShutdownHandler Tests")
class ShutdownHandlerTest {
  @Mock private BoticaClient mockBoticaClient;

  private ShutdownHandler shutdownHandler;

  @BeforeEach
  void setUp() {
    shutdownHandler = new ShutdownHandler(mockBoticaClient);
  }

  @Test
  @DisplayName("constructor should register a ShutdownRequestPacket query listener")
  void constructor_registersShutdownRequestPacketQueryListener() {
    // Arrange
    ArgumentCaptor<RequestPacketListener<ShutdownRequestPacket, ShutdownResponsePacket>> listener =
        captor();

    // Assert
    verify(mockBoticaClient, times(1))
        .registerPacketListener(eq(ShutdownRequestPacket.class), listener.capture());
  }

  @Test
  @DisplayName("onShutdownRequest should return ready=true when no hooks are registered")
  void onShutdownRequest_noHooks_returnsReady() {
    // Arrange
    ShutdownRequestPacket requestPacket = new ShutdownRequestPacket(false);

    // Act
    ShutdownResponsePacket responsePacket = shutdownHandler.onShutdownRequest(requestPacket);

    // Assert
    assertThat(responsePacket.isReady()).isTrue();
  }

  @Test
  @DisplayName("onShutdownRequest should return ready=false if a hook cancels the request")
  void onShutdownRequest_hookCancels_returnsNotReady() {
    // Arrange
    ShutdownRequestPacket requestPacket = new ShutdownRequestPacket(false);
    ShutdownRequestHook cancelingHook = (request, response) -> response.setCanceled(true);
    shutdownHandler.registerShutdownRequestHook(cancelingHook);

    // Act
    ShutdownResponsePacket responsePacket = shutdownHandler.onShutdownRequest(requestPacket);

    // Assert
    assertThat(responsePacket.isReady()).isFalse();
  }

  @Test
  @DisplayName("onShutdownRequest should execute all registered hooks")
  void onShutdownRequest_executesAllHooks() {
    // Arrange
    ShutdownRequestPacket requestPacket = new ShutdownRequestPacket(false);
    ShutdownRequestHook hook1 = mock(ShutdownRequestHook.class);
    ShutdownRequestHook hook2 = mock(ShutdownRequestHook.class);
    shutdownHandler.registerShutdownRequestHook(hook1);
    shutdownHandler.registerShutdownRequestHook(hook2);

    // Act
    shutdownHandler.onShutdownRequest(requestPacket);

    // Assert
    verify(hook1, times(1))
        .onShutdownRequest(any(ShutdownRequest.class), any(ShutdownResponse.class));
    verify(hook2, times(1))
        .onShutdownRequest(any(ShutdownRequest.class), any(ShutdownResponse.class));
  }
}
