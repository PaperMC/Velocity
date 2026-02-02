/*
 * Copyright (C) 2022-2023 Velocity Contributors
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

package com.velocitypowered.proxy.protocol.packet.chat.legacy;

import com.velocitypowered.api.event.player.PlayerChatEvent;
import com.velocitypowered.proxy.VelocityServer;
import com.velocitypowered.proxy.connection.MinecraftConnection;
import com.velocitypowered.proxy.connection.client.ConnectedPlayer;
import com.velocitypowered.proxy.protocol.packet.chat.ChatHandler;

/**
 * A handler for processing legacy chat packets, implementing {@link ChatHandler}.
 *
 * <p>The {@code LegacyChatHandler} is responsible for handling and processing chat messages
 * sent using {@link LegacyChatPacket}. This class provides the necessary logic for
 * processing chat data using legacy Minecraft chat formats.</p>
 */
public class LegacyChatHandler implements ChatHandler<LegacyChatPacket> {

  private final VelocityServer server;
  private final ConnectedPlayer player;

  public LegacyChatHandler(VelocityServer server, ConnectedPlayer player) {
    this.server = server;
    this.player = player;
  }

  @Override
  public Class<LegacyChatPacket> packetClass() {
    return LegacyChatPacket.class;
  }

  @Override
  public void handlePlayerChatInternal(LegacyChatPacket packet) {
    MinecraftConnection serverConnection = player.ensureAndGetCurrentServer().ensureConnected();
    if (serverConnection == null) {
      return;
    }
    this.server.getEventManager().fire(new PlayerChatEvent(this.player, packet.getMessage()))
        .whenComplete((chatEvent, throwable) -> {
          if (!chatEvent.getResult().isAllowed()) {
            return;
          }

          serverConnection.write(this.player.getChatBuilderFactory().builder()
              .message(chatEvent.getResult().getMessage().orElse(packet.getMessage())).toServer());
        });
  }
}
