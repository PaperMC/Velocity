package com.velocitypowered.proxy.connection.backend;

import static com.velocitypowered.proxy.connection.backend.BackendConnectionPhases.IN_TRANSITION;
import static com.velocitypowered.proxy.connection.forge.legacy.LegacyForgeHandshakeBackendPhase.HELLO;

import com.velocitypowered.api.event.player.ServerConnectedEvent;
import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.api.proxy.ConnectionRequestBuilder.Result;
import com.velocitypowered.proxy.VelocityServer;
import com.velocitypowered.proxy.connection.MinecraftConnection;
import com.velocitypowered.proxy.connection.MinecraftSessionHandler;
import com.velocitypowered.proxy.connection.client.ClientPlaySessionHandler;
import com.velocitypowered.proxy.connection.forge.legacy.LegacyForgeConstants;
import com.velocitypowered.proxy.connection.util.ConnectionRequestResults;
import com.velocitypowered.proxy.protocol.packet.Disconnect;
import com.velocitypowered.proxy.protocol.packet.JoinGame;
import com.velocitypowered.proxy.protocol.packet.KeepAlive;
import com.velocitypowered.proxy.protocol.packet.PluginMessage;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;

/**
 * A special session handler that catches "last minute" disconnects.
 */
public class TransitionSessionHandler implements MinecraftSessionHandler {

  private final VelocityServer server;
  private final VelocityServerConnection serverConn;
  private final CompletableFuture<Result> resultFuture;

  /**
   * Creates the new transition handler.
   * @param server the Velocity server instance
   * @param serverConn the server connection
   * @param resultFuture the result future
   */
  public TransitionSessionHandler(VelocityServer server,
      VelocityServerConnection serverConn,
      CompletableFuture<Result> resultFuture) {
    this.server = server;
    this.serverConn = serverConn;
    this.resultFuture = resultFuture;
  }

  @Override
  public boolean beforeHandle() {
    if (!serverConn.isActive()) {
      // Obsolete connection
      serverConn.disconnect();
      return true;
    }
    return false;
  }

  @Override
  public boolean handle(KeepAlive packet) {
    serverConn.ensureConnected().write(packet);
    return true;
  }

  @Override
  public boolean handle(JoinGame packet) {
    MinecraftConnection smc = serverConn.ensureConnected();
    VelocityServerConnection existingConnection = serverConn.getPlayer().getConnectedServer();

    if (existingConnection != null) {
      // Shut down the existing server connection.
      serverConn.getPlayer().setConnectedServer(null);
      existingConnection.disconnect();

      // Send keep alive to try to avoid timeouts
      serverConn.getPlayer().sendKeepAlive();
    }

    // The goods are in hand! We got JoinGame. Let's transition completely to the new state.
    smc.getChannel().config().setAutoRead(false);
    server.getEventManager()
        .fire(new ServerConnectedEvent(serverConn.getPlayer(), serverConn.getServer()))
        .whenCompleteAsync((x, error) -> {
          // Finish up our work. Set the new server and perform switching logic.
          serverConn.getPlayer().setConnectedServer(serverConn);

          // Strap on the ClientPlaySessionHandler if required.
          ClientPlaySessionHandler playHandler;
          if (serverConn.getPlayer().getMinecraftConnection().getSessionHandler()
              instanceof ClientPlaySessionHandler) {
            playHandler = (ClientPlaySessionHandler) serverConn.getPlayer().getMinecraftConnection()
                .getSessionHandler();
          } else {
            playHandler = new ClientPlaySessionHandler(server, serverConn.getPlayer());
            serverConn.getPlayer().getMinecraftConnection().setSessionHandler(playHandler);
          }
          playHandler.handleBackendJoinGame(packet);

          // Strap on the correct session handler for the server. We will have nothing more to do
          // with this connection once this task finishes up.
          smc.setSessionHandler(new BackendPlaySessionHandler(server, serverConn));

          // Clean up disabling auto-read while the connected event was being processed.
          smc.getChannel().config().setAutoRead(true);
          smc.getChannel().read();

          // We're done! :)
          resultFuture.complete(ConnectionRequestResults.successful(serverConn.getServer()));
        }, smc.eventLoop());

    return true;
  }

  @Override
  public boolean handle(Disconnect packet) {
    serverConn.disconnect();
    resultFuture.complete(ConnectionRequestResults.forDisconnect(packet, serverConn.getServer()));
    return true;
  }

  @Override
  public boolean handle(PluginMessage packet) {
    if (!canForwardPluginMessage(packet)) {
      return true;
    }

    // We always need to handle plugin messages, for Forge compatibility.
    System.out.println("BACKEND Current phase: " + serverConn.getPhase());

    if (serverConn.getPhase().handle(serverConn, serverConn.getPlayer(), packet)) {
      System.out.println("BACKEND New phase: " + serverConn.getPhase());

      // Handled, but check the server connection phase.
      if (serverConn.getPhase() == HELLO) {
        VelocityServerConnection existingConnection = serverConn.getPlayer().getConnectedServer();
        if (existingConnection != null && existingConnection.getPhase() != IN_TRANSITION) {
          // Indicate that this connection is "in transition"
          existingConnection.setConnectionPhase(IN_TRANSITION);

          // ..and reset the Forge handshake on the client.
          serverConn.getPlayer().sendLegacyForgeHandshakeResetPacket();
        }
      }
      return true;
    }

    serverConn.getPlayer().getMinecraftConnection().write(packet);
    return false;
  }

  @Override
  public void disconnected() {
    resultFuture
        .completeExceptionally(new IOException("Unexpectedly disconnected from remote server"));
  }

  private boolean canForwardPluginMessage(PluginMessage message) {
    MinecraftConnection mc = serverConn.getConnection();
    if (mc == null) {
      return false;
    }
    boolean minecraftOrFmlMessage;
    if (mc.getProtocolVersion().compareTo(ProtocolVersion.MINECRAFT_1_12_2) <= 0) {
      String channel = message.getChannel();
      minecraftOrFmlMessage = channel.startsWith("MC|") || channel
          .startsWith(LegacyForgeConstants.FORGE_LEGACY_HANDSHAKE_CHANNEL);
    } else {
      minecraftOrFmlMessage = message.getChannel().startsWith("minecraft:");
    }
    return minecraftOrFmlMessage
        || server.getChannelRegistrar().registered(message.getChannel());
  }
}
