package com.velocitypowered.proxy.connection.backend;

import static com.velocitypowered.proxy.connection.backend.BackendConnectionPhases.IN_TRANSITION;
import static com.velocitypowered.proxy.connection.forge.legacy.LegacyForgeHandshakeBackendPhase.HELLO;

import com.google.common.collect.ImmutableList;
import com.velocitypowered.api.event.player.ServerConnectedEvent;
import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.proxy.VelocityServer;
import com.velocitypowered.proxy.connection.ConnectionTypes;
import com.velocitypowered.proxy.connection.MinecraftConnection;
import com.velocitypowered.proxy.connection.MinecraftSessionHandler;
import com.velocitypowered.proxy.connection.client.ClientPlaySessionHandler;
import com.velocitypowered.proxy.connection.client.InitialConnectSessionHandler;
import com.velocitypowered.proxy.connection.forge.legacy.LegacyForgeConstants;
import com.velocitypowered.proxy.connection.util.ConnectionRequestResults;
import com.velocitypowered.proxy.connection.util.ConnectionRequestResults.Impl;
import com.velocitypowered.proxy.protocol.packet.Disconnect;
import com.velocitypowered.proxy.protocol.packet.JoinGame;
import com.velocitypowered.proxy.protocol.packet.KeepAlive;
import com.velocitypowered.proxy.protocol.packet.PluginMessage;
import com.velocitypowered.proxy.protocol.util.PluginMessageUtil;
import java.io.IOException;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;

/**
 * A special session handler that catches "last minute" disconnects.
 */
public class TransitionSessionHandler implements MinecraftSessionHandler {

  private final VelocityServer server;
  private final VelocityServerConnection serverConn;
  private final CompletableFuture<Impl> resultFuture;

  /**
   * Creates the new transition handler.
   * @param server the Velocity server instance
   * @param serverConn the server connection
   * @param resultFuture the result future
   */
  TransitionSessionHandler(VelocityServer server,
      VelocityServerConnection serverConn,
      CompletableFuture<Impl> resultFuture) {
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
    smc.setAutoReading(false);
    server.getEventManager()
        .fire(new ServerConnectedEvent(serverConn.getPlayer(), serverConn.getServer()))
        .whenCompleteAsync((x, error) -> {
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
          playHandler.handleBackendJoinGame(packet, serverConn);

          // Strap on the correct session handler for the server. We will have nothing more to do
          // with this connection once this task finishes up.
          smc.setSessionHandler(new BackendPlaySessionHandler(server, serverConn));

          // Clean up disabling auto-read while the connected event was being processed.
          smc.setAutoReading(true);

          // Now set the connected server.
          serverConn.getPlayer().setConnectedServer(serverConn);

          // We're done! :)
          resultFuture.complete(ConnectionRequestResults.successful(serverConn.getServer()));
        }, smc.eventLoop());

    return true;
  }

  @Override
  public boolean handle(Disconnect packet) {
    final MinecraftConnection connection = serverConn.ensureConnected();
    serverConn.disconnect();

    // If we were in the middle of the Forge handshake, it is not safe to proceed. We must kick
    // the client.
    if (connection.getType() == ConnectionTypes.LEGACY_FORGE
        && !serverConn.getPhase().consideredComplete()) {
      resultFuture.complete(ConnectionRequestResults.forUnsafeDisconnect(packet,
          serverConn.getServer()));
    } else {
      resultFuture.complete(ConnectionRequestResults.forDisconnect(packet, serverConn.getServer()));
    }

    return true;
  }

  @Override
  public boolean handle(PluginMessage packet) {
    if (!serverConn.getPlayer().canForwardPluginMessage(packet)) {
      return true;
    }

    // We need to specially handle REGISTER and UNREGISTER packets. Later on, we'll write them to
    // the client.
    if (PluginMessageUtil.isRegister(packet)) {
      serverConn.getPlayer().getKnownChannels().addAll(PluginMessageUtil.getChannels(packet));
    } else if (PluginMessageUtil.isUnregister(packet)) {
      serverConn.getPlayer().getKnownChannels().removeAll(PluginMessageUtil.getChannels(packet));
    }

    // We always need to handle plugin messages, for Forge compatibility.
    if (serverConn.getPhase().handle(serverConn, serverConn.getPlayer(), packet)) {
      // Handled, but check the server connection phase.
      if (serverConn.getPhase() == HELLO) {
        VelocityServerConnection existingConnection = serverConn.getPlayer().getConnectedServer();
        if (existingConnection != null && existingConnection.getPhase() != IN_TRANSITION) {
          // Indicate that this connection is "in transition"
          existingConnection.setConnectionPhase(IN_TRANSITION);

          // Tell the player that we're leaving and we just aren't coming back.
          existingConnection.getPhase().onDepartForNewServer(existingConnection,
              serverConn.getPlayer());
        }
      }
      return true;
    }

    serverConn.getPlayer().getMinecraftConnection().write(packet);
    return true;
  }

  @Override
  public void disconnected() {
    resultFuture
        .completeExceptionally(new IOException("Unexpectedly disconnected from remote server"));
  }
}
