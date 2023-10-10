/*
 * Copyright (C) 2018-2023 Velocity Contributors
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

package com.velocitypowered.proxy.connection.client;

import com.velocitypowered.proxy.VelocityServer;
import com.velocitypowered.proxy.connection.MinecraftConnection;
import com.velocitypowered.proxy.connection.MinecraftSessionHandler;
import com.velocitypowered.proxy.connection.backend.VelocityServerConnection;
import com.velocitypowered.proxy.protocol.MinecraftPacket;
import com.velocitypowered.proxy.protocol.StateRegistry;
import com.velocitypowered.proxy.protocol.packet.ClientSettings;
import com.velocitypowered.proxy.protocol.packet.KeepAlive;
import com.velocitypowered.proxy.protocol.packet.PluginMessage;
import com.velocitypowered.proxy.protocol.packet.ResourcePackResponse;
import com.velocitypowered.proxy.protocol.packet.config.FinishedUpdate;
import io.netty.buffer.ByteBuf;
import java.util.concurrent.CompletableFuture;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Handles the client config stage.
 */
public class ClientConfigSessionHandler implements MinecraftSessionHandler {

  private static final Logger logger = LogManager.getLogger(ClientConfigSessionHandler.class);
  private final VelocityServer server;
  private final ConnectedPlayer player;

  private CompletableFuture<Void> configSwitchFuture;

  /**
   * Constructs a client config session handler.
   *
   * @param server the Velocity server instance
   * @param player the player
   */
  public ClientConfigSessionHandler(VelocityServer server, ConnectedPlayer player) {
    this.server = server;
    this.player = player;
  }

  @Override
  public void activated() {
    configSwitchFuture = new CompletableFuture<>();
  }

  @Override
  public void deactivated() {
  }

  @Override
  public boolean handle(KeepAlive packet) {
    VelocityServerConnection serverConnection = player.getConnectedServer();
    if (serverConnection != null) {
      Long sentTime = serverConnection.getPendingPings().remove(packet.getRandomId());
      if (sentTime != null) {
        MinecraftConnection smc = serverConnection.getConnection();
        if (smc != null) {
          player.setPing(System.currentTimeMillis() - sentTime);
          smc.write(packet);
        }
      }
    }
    return true;
  }

  @Override
  public boolean handle(ClientSettings packet) {
    player.setClientSettingsPacket(packet);
    return true;
  }

  @Override
  public boolean handle(ResourcePackResponse packet) {
    if (player.getConnectionInFlight() != null) {
      player.getConnectionInFlight().ensureConnected().write(packet);
    }
    return player.onResourcePackResponse(packet.getStatus());
  }

  @Override
  public boolean handle(FinishedUpdate packet) {
    player.getConnection()
        .setActiveSessionHandler(StateRegistry.PLAY, new ClientPlaySessionHandler(server, player));

    configSwitchFuture.complete(null);
    return true;
  }

  @Override
  public void handleGeneric(MinecraftPacket packet) {
    VelocityServerConnection serverConnection = player.getConnectedServer();
    if (serverConnection == null) {
      // No server connection yet, probably transitioning.
      return;
    }

    MinecraftConnection smc = serverConnection.getConnection();
    if (smc != null && serverConnection.getPhase().consideredComplete()) {
      if (packet instanceof PluginMessage) {
        ((PluginMessage) packet).retain();
      }
      smc.write(packet);
    }
  }

  @Override
  public void handleUnknown(ByteBuf buf) {
    VelocityServerConnection serverConnection = player.getConnectedServer();
    if (serverConnection == null) {
      // No server connection yet, probably transitioning.
      return;
    }

    MinecraftConnection smc = serverConnection.getConnection();
    if (smc != null && !smc.isClosed() && serverConnection.getPhase().consideredComplete()) {
      smc.write(buf.retain());
    }
  }

  @Override
  public void disconnected() {
    player.teardown();
  }

  @Override
  public void exception(Throwable throwable) {
    player.disconnect(
        Component.translatable("velocity.error.player-connection-error", NamedTextColor.RED));
  }

  /**
   * Handles the backend finishing the config stage.
   *
   * @param serverConn the server connection
   * @return a future that completes when the config stage is finished
   */
  public CompletableFuture<Void> handleBackendFinishUpdate(VelocityServerConnection serverConn) {
    player.getConnection().write(new FinishedUpdate());
    serverConn.ensureConnected().write(new FinishedUpdate());
    return configSwitchFuture;
  }
}
