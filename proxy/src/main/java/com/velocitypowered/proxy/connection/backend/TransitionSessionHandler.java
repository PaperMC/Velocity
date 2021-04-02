/*
 * Copyright (C) 2018 Velocity Contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.velocitypowered.proxy.connection.backend;

import static com.velocitypowered.proxy.connection.backend.BackendConnectionPhases.IN_TRANSITION;
import static com.velocitypowered.proxy.connection.forge.legacy.LegacyForgeHandshakeBackendPhase.HELLO;

import com.velocitypowered.api.event.player.ServerConnectedEvent;
import com.velocitypowered.api.event.player.ServerPostConnectEvent;
import com.velocitypowered.proxy.VelocityServer;
import com.velocitypowered.proxy.connection.ConnectionTypes;
import com.velocitypowered.proxy.connection.MinecraftConnection;
import com.velocitypowered.proxy.connection.MinecraftSessionHandler;
import com.velocitypowered.proxy.connection.client.ClientPlaySessionHandler;
import com.velocitypowered.proxy.connection.client.ConnectedPlayer;
import com.velocitypowered.proxy.connection.util.ConnectionMessages;
import com.velocitypowered.proxy.connection.util.ConnectionRequestResults;
import com.velocitypowered.proxy.connection.util.ConnectionRequestResults.Impl;
import com.velocitypowered.proxy.protocol.packet.Disconnect;
import com.velocitypowered.proxy.protocol.packet.JoinGame;
import com.velocitypowered.proxy.protocol.packet.KeepAlive;
import com.velocitypowered.proxy.protocol.packet.PluginMessage;
import com.velocitypowered.proxy.protocol.util.PluginMessageUtil;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * A special session handler that catches "last minute" disconnects.
 */
public class TransitionSessionHandler implements MinecraftSessionHandler {

  private static final Logger logger = LogManager.getLogger(TransitionSessionHandler.class);

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

    final ConnectedPlayer player = serverConn.getPlayer();

    if (existingConnection != null) {
      // Shut down the existing server connection.
      player.setConnectedServer(null);
      existingConnection.disconnect();

      // Send keep alive to try to avoid timeouts
      player.sendKeepAlive();
    }

    // The goods are in hand! We got JoinGame. Let's transition completely to the new state.
    smc.setAutoReading(false);
    server.getEventManager()
        .fire(new ServerConnectedEvent(player, serverConn.getServer(),
            existingConnection != null ? existingConnection.getServer() : null))
        .thenRunAsync(() -> {
          // Make sure we can still transition (player might have disconnected here).
          if (!serverConn.isActive()) {
            // Connection is obsolete.
            serverConn.disconnect();
            return;
          }

          // Change the client to use the ClientPlaySessionHandler if required.
          ClientPlaySessionHandler playHandler;
          if (player.getConnection().getSessionHandler() instanceof ClientPlaySessionHandler) {
            playHandler = (ClientPlaySessionHandler) player.getConnection().getSessionHandler();
          } else {
            playHandler = new ClientPlaySessionHandler(server, player);
            player.getConnection().setSessionHandler(playHandler);
          }
          playHandler.handleBackendJoinGame(packet, serverConn);

          // Set the new play session handler for the server. We will have nothing more to do
          // with this connection once this task finishes up.
          smc.setSessionHandler(new BackendPlaySessionHandler(server, serverConn));

          // Clean up disabling auto-read while the connected event was being processed.
          smc.setAutoReading(true);

          // Now set the connected server.
          serverConn.getPlayer().setConnectedServer(serverConn);

          // We're done! :)
          server.getEventManager().fireAndForget(new ServerPostConnectEvent(player,
              existingConnection == null ? null : existingConnection.getServer()));
          resultFuture.complete(ConnectionRequestResults.successful(serverConn.getServer()));
        }, smc.eventLoop())
        .exceptionally(exc -> {
          logger.error("Unable to switch to new server {} for {}",
              serverConn.getServerInfo().getName(),
              player.getUsername(), exc);
          player.disconnect(ConnectionMessages.INTERNAL_SERVER_CONNECTION_ERROR);
          resultFuture.completeExceptionally(exc);
          return null;
        });

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
    if (!serverConn.getPlayer().canForwardPluginMessage(serverConn.ensureConnected()
        .getProtocolVersion(), packet)) {
      return true;
    }

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

    serverConn.getPlayer().getConnection().write(packet.retain());
    return true;
  }

  @Override
  public void disconnected() {
    resultFuture
        .completeExceptionally(new IOException("Unexpectedly disconnected from remote server"));
  }
}
