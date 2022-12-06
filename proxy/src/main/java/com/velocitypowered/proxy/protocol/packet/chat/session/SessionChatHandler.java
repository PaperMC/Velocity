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

package com.velocitypowered.proxy.protocol.packet.chat.session;

import static com.velocitypowered.proxy.protocol.packet.chat.keyed.KeyedChatHandler.invalidCancel;
import static com.velocitypowered.proxy.protocol.packet.chat.keyed.KeyedChatHandler.invalidChange;

import com.velocitypowered.api.event.EventManager;
import com.velocitypowered.api.event.player.PlayerChatEvent;
import com.velocitypowered.proxy.VelocityServer;
import com.velocitypowered.proxy.connection.client.ConnectedPlayer;
import com.velocitypowered.proxy.protocol.packet.chat.ChatHandler;
import com.velocitypowered.proxy.protocol.packet.chat.ChatQueue;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class SessionChatHandler implements ChatHandler<SessionPlayerChat> {
  private static final Logger logger = LogManager.getLogger(SessionChatHandler.class);

  private final ConnectedPlayer player;
  private final VelocityServer server;

  public SessionChatHandler(ConnectedPlayer player, VelocityServer server) {
    this.player = player;
    this.server = server;
  }

  @Override
  public Class<SessionPlayerChat> packetClass() {
    return SessionPlayerChat.class;
  }

  @Override
  public void handlePlayerChatInternal(SessionPlayerChat packet) {
    ChatQueue chatQueue = this.player.getChatQueue();
    EventManager eventManager = this.server.getEventManager();
    PlayerChatEvent toSend = new PlayerChatEvent(player, packet.getMessage());
    chatQueue.queuePacket(
        eventManager.fire(toSend)
            .thenApply(pme -> {
              PlayerChatEvent.ChatResult chatResult = pme.getResult();
              if (!chatResult.isAllowed()) {
                if (packet.isSigned()) {
                  invalidCancel(logger, player);
                }
                return null;
              }

              if (chatResult.getMessage().map(str -> !str.equals(packet.getMessage())).orElse(false)) {
                if (packet.isSigned()) {
                  invalidChange(logger, player);
                  return null;
                }
                return this.player.getChatBuilderFactory().builder().message(packet.message)
                    .setTimestamp(packet.timestamp)
                    .toServer();
              }
              return packet;
            })
            .exceptionally((ex) -> {
              logger.error("Exception while handling player chat for {}", player, ex);
              return null;
            }),
        packet.getTimestamp()
    );
  }
}
