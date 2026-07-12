/*
 * Copyright (C) 2026 Velocity Contributors
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

package com.velocitypowered.proxy.protocol.packet.chat;

import com.velocitypowered.api.event.player.PlayerChatForwarding;
import com.velocitypowered.api.event.player.PlayerChatMessage;
import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.proxy.connection.client.ConnectedPlayer;
import net.kyori.adventure.text.Component;
import org.apache.logging.log4j.Logger;

/**
 * Emits decorated modern player chat directly to client recipients.
 */
public final class DecoratedPlayerChatForwarder {

  private DecoratedPlayerChatForwarder() {
  }

  public static void forward(PlayerChatMessage originalMessage, PlayerChatForwarding forwarding,
      Logger logger) {
    Component senderName = Component.text(originalMessage.getSender().getUsername());
    for (Player recipient : forwarding.getRecipients()) {
      if (!(recipient instanceof ConnectedPlayer connectedPlayer)) {
        continue;
      }
      ProtocolVersion protocolVersion = connectedPlayer.getProtocolVersion();
      if (!protocolVersion.noLessThan(ProtocolVersion.MINECRAFT_1_21_5)) {
        logger.warn("Cannot emit decorated player chat for {} to {}: protocol {} is not "
                + "supported by Velocity's clientbound player-chat emitter",
            originalMessage.getSender().getUsername(), connectedPlayer.getUsername(),
            protocolVersion);
        continue;
      }
      connectedPlayer.getConnection().write(new ClientboundPlayerChatPacket(originalMessage,
          forwarding.getDecoratedMessage(), senderName, protocolVersion));
    }
  }
}
