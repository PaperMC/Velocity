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

import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.proxy.messages.ChannelIdentifier;
import com.velocitypowered.proxy.VelocityServer;
import com.velocitypowered.proxy.connection.MinecraftSessionHandler;
import com.velocitypowered.proxy.connection.backend.BungeeCordMessageResponder;
import com.velocitypowered.proxy.connection.backend.VelocityServerConnection;
import com.velocitypowered.proxy.protocol.packet.PluginMessage;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Handles the play state between exiting the login phase and establishing the first connection
 * to a backend server.
 */
public class InitialConnectSessionHandler implements MinecraftSessionHandler {

  private static final Logger logger = LogManager.getLogger(InitialConnectSessionHandler.class);

  private final ConnectedPlayer player;

  private final VelocityServer server;

  InitialConnectSessionHandler(ConnectedPlayer player, VelocityServer server) {
    this.player = player;
    this.server = server;
  }

  @Override
  public boolean handle(PluginMessage packet) {
    VelocityServerConnection serverConn = player.getConnectionInFlight();
    if (serverConn != null) {
      if (player.getPhase().handle(player, packet, serverConn)) {
        return true;
      }

      if (BungeeCordMessageResponder.isBungeeCordMessage(packet)) {
        return true;
      }

      ChannelIdentifier id = server.getChannelRegistrar().getFromId(packet.getChannel());
      if (id == null) {
        serverConn.ensureConnected().write(packet.retain());
        return true;
      }

      byte[] copy = ByteBufUtil.getBytes(packet.content());
      PluginMessageEvent event = new PluginMessageEvent(serverConn, serverConn.getPlayer(), id,
          copy);
      server.getEventManager().fire(event)
          .thenAcceptAsync(pme -> {
            if (pme.getResult().isAllowed() && serverConn.isActive()) {
              PluginMessage copied = new PluginMessage(packet.getChannel(),
                  Unpooled.wrappedBuffer(copy));
              serverConn.ensureConnected().write(copied);
            }
          }, player.getConnection().eventLoop())
          .exceptionally((ex) -> {
            logger.error("Exception while handling plugin message {}", packet, ex);
            return null;
          });
    }
    return true;
  }

  @Override
  public void disconnected() {
    // the user cancelled the login process
    player.teardown();
  }
}
