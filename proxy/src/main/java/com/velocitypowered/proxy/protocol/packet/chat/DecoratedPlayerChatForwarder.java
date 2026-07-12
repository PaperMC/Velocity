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
import com.velocitypowered.api.event.player.PlayerChatProtocol;
import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.proxy.connection.client.ConnectedPlayer;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import com.velocitypowered.proxy.protocol.packet.chat.legacy.LegacyChatPacket;
import net.kyori.adventure.text.Component;
import org.apache.logging.log4j.Logger;

/**
 * Emits decorated modern player chat directly to client recipients.
 */
public final class DecoratedPlayerChatForwarder {

  private DecoratedPlayerChatForwarder() {
  }

  public static Result forward(PlayerChatMessage originalMessage, PlayerChatForwarding forwarding,
      Logger logger) {
    if (!canEmitAsDecoratedPlayerChat(originalMessage)) {
      logger.warn("Cannot emit decorated player chat for {}: original message is {} / {} and "
              + "cannot be represented as signature-preserving clientbound player chat",
          originalMessage.getSender().getUsername(), originalMessage.getProtocol(),
          originalMessage.getSignedState());
      return Result.UNSUPPORTED_MESSAGE;
    }

    Component senderName = Component.text(originalMessage.getSender().getUsername());
    boolean delivered = false;
    for (Player recipient : forwarding.getRecipients()) {
      if (!(recipient instanceof ConnectedPlayer connectedPlayer)) {
        continue;
      }
      ProtocolVersion protocolVersion = connectedPlayer.getProtocolVersion();
      if (!ClientboundPlayerChatPacket.supportsProtocol(protocolVersion)) {
        logger.warn("Cannot emit decorated player chat for {} to {}: protocol {} is not "
                + "supported by Velocity's clientbound player-chat emitter",
            originalMessage.getSender().getUsername(), connectedPlayer.getUsername(),
            protocolVersion);
        continue;
      }
      connectedPlayer.getConnection().write(new ClientboundPlayerChatPacket(originalMessage,
          forwarding.getDecoratedMessage(), senderName, protocolVersion));
      delivered = true;
    }
    return delivered ? Result.DELIVERED : Result.NO_SUPPORTED_RECIPIENTS;
  }

  public static Result forwardLegacy(PlayerChatMessage originalMessage,
      PlayerChatForwarding forwarding) {
    boolean delivered = false;
    for (Player recipient : forwarding.getRecipients()) {
      if (!(recipient instanceof ConnectedPlayer connectedPlayer)) {
        continue;
      }
      ProtocolVersion version = connectedPlayer.getProtocolVersion();
      Component translated = connectedPlayer.translateMessage(forwarding.getDecoratedMessage());
      String json = ProtocolUtils.getJsonChatSerializer(version).serialize(translated);
      connectedPlayer.getConnection().write(new LegacyChatPacket(json, LegacyChatPacket.CHAT_TYPE,
          originalMessage.getSender().getUniqueId()));
      delivered = true;
    }
    return delivered ? Result.DELIVERED : Result.NO_SUPPORTED_RECIPIENTS;
  }

  static boolean canEmitAsDecoratedPlayerChat(PlayerChatMessage originalMessage) {
    return originalMessage.getProtocol() == PlayerChatProtocol.SESSION_CHAT
        && originalMessage.getSignature().flatMap(signature -> signature.getTimestamp()).isPresent()
        && originalMessage.getSignature().flatMap(signature -> signature.getSalt()).isPresent()
        && originalMessage.getSignature().map(signature -> signature.getSignature().length == 256)
            .orElse(false);
  }

  public enum Result {
    DELIVERED,
    NO_SUPPORTED_RECIPIENTS,
    UNSUPPORTED_MESSAGE
  }
}
