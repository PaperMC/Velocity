package com.velocitypowered.proxy.connection.backend;

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
  private final ClientPlaySessionHandler playerSessionHandler;

  public TransitionSessionHandler(VelocityServer server,
      VelocityServerConnection serverConn,
      CompletableFuture<Result> resultFuture) {
    this.server = server;
    this.serverConn = serverConn;
    this.resultFuture = resultFuture;

    MinecraftSessionHandler psh = serverConn.getPlayer().getMinecraftConnection()
        .getSessionHandler();
    if (!(psh instanceof ClientPlaySessionHandler)) {
      throw new IllegalStateException(
          "Initializing BackendPlaySessionHandler with no backing client play session handler!");
    }
    this.playerSessionHandler = (ClientPlaySessionHandler) psh;
  }

  private MinecraftConnection ensureMinecraftConnection() {
    MinecraftConnection mc = serverConn.getConnection();
    if (mc == null) {
      throw new IllegalStateException("Not connected to backend server!");
    }
    return mc;
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
  public boolean handle(JoinGame packet) {
    MinecraftConnection smc = ensureMinecraftConnection();
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
          // Finish up our work
          serverConn.getPlayer().setConnectedServer(serverConn);

          playerSessionHandler.handleBackendJoinGame(packet);
          smc.setSessionHandler(new BackendPlaySessionHandler(server, serverConn));

          resultFuture.complete(ConnectionRequestResults.successful(serverConn.getServer()));
          smc.getChannel().config().setAutoRead(true);
          smc.getChannel().read();
        }, smc.eventLoop());

    return true;
  }

  @Override
  public boolean handle(Disconnect packet) {
    resultFuture.complete(ConnectionRequestResults.forDisconnect(packet, serverConn.getServer()));
    serverConn.disconnect();
    return true;
  }

  @Override
  public boolean handle(PluginMessage packet) {
    if (!canForwardPluginMessage(packet)) {
      return true;
    }

    // We always need to handle plugin messages, for Forge compatibility.
    if (serverConn.getPhase().handle(serverConn, serverConn.getPlayer(), packet)) {
      // Handled.
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
        || playerSessionHandler.getKnownChannels().contains(message.getChannel())
        || server.getChannelRegistrar().registered(message.getChannel());
  }
}
