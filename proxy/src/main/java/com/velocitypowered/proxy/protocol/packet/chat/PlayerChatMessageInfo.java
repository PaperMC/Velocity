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

import com.velocitypowered.api.event.player.PlayerChatEvent;
import com.velocitypowered.api.proxy.crypto.IdentifiedKey;
import com.velocitypowered.proxy.connection.client.ConnectedPlayer;
import com.velocitypowered.proxy.crypto.SignedPlayerMessage;
import com.velocitypowered.proxy.protocol.packet.chat.keyed.KeyedPlayerChatPacket;
import com.velocitypowered.proxy.protocol.packet.chat.session.SessionPlayerChatPacket;

/**
 * Creates public chat metadata from internal chat packets.
 */
public final class PlayerChatMessageInfo {

  private PlayerChatMessageInfo() {
  }

  public static PlayerChatEvent.MessageInfo fromSessionPacket(ConnectedPlayer player,
      SessionPlayerChatPacket packet) {
    if (!packet.isSigned()) {
      return PlayerChatEvent.MessageInfo.unsigned();
    }

    IdentifiedKey key = player.getIdentifiedKey();
    if (key == null) {
      return PlayerChatEvent.MessageInfo.signed();
    }

    return PlayerChatEvent.MessageInfo.signed(new SignedPlayerMessage(packet.getMessage(),
        key.getSignedPublicKey(), player.getUniqueId(), key.getExpiryTemporal(),
        packet.getSignature(), packet.getSaltBytes(), false));
  }

  public static PlayerChatEvent.MessageInfo fromKeyedPacket(ConnectedPlayer player,
      KeyedPlayerChatPacket packet) {
    if (packet.isUnsigned()) {
      return PlayerChatEvent.MessageInfo.unsigned();
    }

    IdentifiedKey key = player.getIdentifiedKey();
    if (key == null || packet.getExpiry() == null) {
      return PlayerChatEvent.MessageInfo.signed();
    }

    return PlayerChatEvent.MessageInfo.signed(new SignedPlayerMessage(packet.getMessage(),
        key.getSignedPublicKey(), player.getUniqueId(), packet.getExpiry(),
        packet.getSignature(), packet.getSalt(), packet.isSignedPreview()));
  }
}
