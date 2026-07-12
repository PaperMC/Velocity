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
import com.velocitypowered.api.event.player.PlayerChatCapabilities;
import com.velocitypowered.api.event.player.PlayerChatChainInfo;
import com.velocitypowered.api.event.player.PlayerChatKeyInfo;
import com.velocitypowered.api.event.player.PlayerChatMessage;
import com.velocitypowered.api.event.player.PlayerChatMessageLink;
import com.velocitypowered.api.event.player.PlayerChatProtocol;
import com.velocitypowered.api.event.player.PlayerChatSessionInfo;
import com.velocitypowered.api.event.player.PlayerChatSignature;
import com.velocitypowered.api.event.player.PlayerChatSignedState;
import com.velocitypowered.api.event.player.PlayerChatValidationFlag;
import com.velocitypowered.api.proxy.crypto.IdentifiedKey;
import com.velocitypowered.api.proxy.player.ChatSession;
import com.velocitypowered.api.proxy.player.TabListEntry;
import com.velocitypowered.proxy.connection.client.ConnectedPlayer;
import com.velocitypowered.proxy.crypto.SignaturePair;
import com.velocitypowered.proxy.protocol.packet.chat.keyed.KeyedPlayerChatPacket;
import com.velocitypowered.proxy.protocol.packet.chat.session.SessionPlayerChatPacket;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;

/**
 * Creates public chat metadata from internal chat packets.
 */
public final class PlayerChatMessageInfo {

  private PlayerChatMessageInfo() {
  }

  public static PlayerChatEvent.MessageInfo fromSessionPacket(ConnectedPlayer player,
      SessionPlayerChatPacket packet) {
    return PlayerChatEvent.MessageInfo.fromChatMessage(sessionMessage(player, packet));
  }

  public static PlayerChatMessage sessionMessage(ConnectedPlayer player,
      SessionPlayerChatPacket packet) {
    PlayerChatChainInfo chainInfo = lastSeen(packet.getLastSeenMessages());
    if (!packet.isSigned()) {
      return PlayerChatMessage.unsigned(player, packet.getMessage(), PlayerChatProtocol.SESSION_CHAT,
          chainInfo);
    }

    PlayerChatSignature signature = new PlayerChatSignature(packet.getSignature(),
        packet.getTimestamp(), packet.getSalt(), packet.getSaltBytes(), false);
    EnumSet<PlayerChatValidationFlag> flags = signedFlags();
    flags.add(PlayerChatValidationFlag.CHAIN_STATE_AVAILABLE);

    Optional<ChatSession> chatSession = activeChatSession(player);
    if (chatSession.isEmpty() || chatSession.get().getSessionId() == null) {
      return new PlayerChatMessage(player, packet.getMessage(), PlayerChatProtocol.SESSION_CHAT,
          PlayerChatSignedState.SIGNED, signature, null, null, chainInfo, flags,
          PlayerChatCapabilities.signedPassthrough());
    }

    ChatSession session = chatSession.get();
    IdentifiedKey key = session.getIdentifiedKey();
    if (key == null) {
      return new PlayerChatMessage(player, packet.getMessage(), PlayerChatProtocol.SESSION_CHAT,
          PlayerChatSignedState.SIGNED, signature, null, null, chainInfo, flags,
          PlayerChatCapabilities.signedPassthrough());
    }
    PlayerChatSessionInfo sessionInfo = new PlayerChatSessionInfo(session.getSessionId(),
        key.getSignedPublicKey(), key.getExpiryTemporal(), key.getSignatureHolder());
    flags.add(PlayerChatValidationFlag.KEY_AVAILABLE);
    flags.add(PlayerChatValidationFlag.STRUCTURALLY_COMPLETE);
    if (player.getUniqueId().equals(key.getSignatureHolder())) {
      flags.add(PlayerChatValidationFlag.SESSION_MATCHED);
    }

    return new PlayerChatMessage(player, packet.getMessage(), PlayerChatProtocol.SESSION_CHAT,
        PlayerChatSignedState.SESSION_SIGNED, signature, null, sessionInfo, chainInfo, flags,
        PlayerChatCapabilities.signedPassthrough());
  }

  public static PlayerChatEvent.MessageInfo fromKeyedPacket(ConnectedPlayer player,
      KeyedPlayerChatPacket packet) {
    return PlayerChatEvent.MessageInfo.fromChatMessage(keyedMessage(player, packet));
  }

  public static PlayerChatMessage keyedMessage(ConnectedPlayer player,
      KeyedPlayerChatPacket packet) {
    PlayerChatChainInfo chainInfo = keyedChain(packet);
    if (packet.isUnsigned()) {
      return PlayerChatMessage.unsigned(player, packet.getMessage(), PlayerChatProtocol.KEYED_CHAT,
          chainInfo);
    }

    PlayerChatSignature signature = new PlayerChatSignature(packet.getSignature(),
        packet.getExpiry(), null, packet.getSalt(), packet.isSignedPreview());
    EnumSet<PlayerChatValidationFlag> flags = signedFlags();
    if (chainInfo != null) {
      flags.add(PlayerChatValidationFlag.CHAIN_STATE_AVAILABLE);
    }
    IdentifiedKey key = player.getIdentifiedKey();
    if (key == null || packet.getExpiry() == null) {
      return new PlayerChatMessage(player, packet.getMessage(), PlayerChatProtocol.KEYED_CHAT,
          PlayerChatSignedState.SIGNED, signature, null, null, chainInfo, flags,
          PlayerChatCapabilities.signedPassthrough());
    }

    PlayerChatKeyInfo keyInfo = new PlayerChatKeyInfo(key.getSignedPublicKey(),
        packet.getExpiry(), key.getSignatureHolder());
    flags.add(PlayerChatValidationFlag.KEY_AVAILABLE);
    flags.add(PlayerChatValidationFlag.STRUCTURALLY_COMPLETE);

    return new PlayerChatMessage(player, packet.getMessage(), PlayerChatProtocol.KEYED_CHAT,
        PlayerChatSignedState.KEYED_SIGNED, signature, keyInfo, null, chainInfo, flags,
        PlayerChatCapabilities.signedPassthrough());
  }

  public static PlayerChatMessage legacyMessage(ConnectedPlayer player, String message) {
    return PlayerChatMessage.legacy(player, message);
  }

  private static Optional<ChatSession> activeChatSession(ConnectedPlayer player) {
    return player.getTabList().getEntry(player.getUniqueId())
        .map(TabListEntry::getChatSession);
  }

  private static PlayerChatChainInfo lastSeen(LastSeenMessages lastSeenMessages) {
    if (lastSeenMessages == null) {
      return null;
    }
    return new PlayerChatChainInfo(lastSeenMessages.getOffset(), lastSeenMessages.getAcknowledged(),
        lastSeenMessages.hasChecksum() ? lastSeenMessages.getChecksum() : null, List.of(), null);
  }

  private static PlayerChatChainInfo keyedChain(KeyedPlayerChatPacket packet) {
    List<PlayerChatMessageLink> previousMessages = Arrays.stream(packet.getPreviousMessages())
        .map(PlayerChatMessageInfo::link)
        .toList();
    SignaturePair lastMessage = packet.getLastMessage();
    if (previousMessages.isEmpty() && lastMessage == null) {
      return null;
    }
    return new PlayerChatChainInfo(null, null, null, previousMessages,
        lastMessage == null ? null : link(lastMessage));
  }

  private static PlayerChatMessageLink link(SignaturePair pair) {
    return new PlayerChatMessageLink(pair.getSigner(), pair.getSignature());
  }

  private static EnumSet<PlayerChatValidationFlag> signedFlags() {
    return EnumSet.of(PlayerChatValidationFlag.SIGNATURE_PRESENT,
        PlayerChatValidationFlag.VALIDATION_UNAVAILABLE);
  }
}
