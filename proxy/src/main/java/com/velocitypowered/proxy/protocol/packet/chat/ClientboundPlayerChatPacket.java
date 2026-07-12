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

import com.velocitypowered.api.event.player.PlayerChatMessage;
import com.velocitypowered.api.event.player.PlayerChatProtocol;
import com.velocitypowered.api.event.player.PlayerChatSignature;
import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.proxy.connection.MinecraftSessionHandler;
import com.velocitypowered.proxy.protocol.MinecraftPacket;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import io.netty.buffer.ByteBuf;
import java.time.Instant;
import java.util.UUID;
import net.kyori.adventure.text.Component;

/**
 * Clientbound modern player-chat packet used for proxy-side decorated player-chat emission.
 */
public final class ClientboundPlayerChatPacket implements MinecraftPacket {

  private static final int CHAT_TYPE_CHAT_HOLDER_ID = 1;

  private int globalIndex;
  private UUID sender;
  private int index;
  private byte[] signature;
  private String message;
  private Instant timestamp;
  private long salt;
  private ComponentHolder unsignedContent;
  private ComponentHolder senderName;

  public ClientboundPlayerChatPacket() {
  }

  public ClientboundPlayerChatPacket(PlayerChatMessage originalMessage, Component decoratedMessage,
      Component senderName, ProtocolVersion version) {
    if (!supportsProtocol(version)) {
      throw new IllegalArgumentException("Clientbound player chat is not supported for " + version);
    }
    if (originalMessage.getProtocol() != PlayerChatProtocol.SESSION_CHAT) {
      throw new IllegalArgumentException("Only modern session chat can be emitted as decorated "
          + "clientbound player chat");
    }
    this.sender = originalMessage.getSender().getUniqueId();
    this.signature = originalMessage.getSignature()
        .map(PlayerChatSignature::getSignature)
        .orElse(null);
    this.message = originalMessage.getMessage();
    this.timestamp = originalMessage.getSignature()
        .flatMap(PlayerChatSignature::getTimestamp)
        .orElse(Instant.now());
    this.salt = originalMessage.getSignature()
        .flatMap(PlayerChatSignature::getSalt)
        .orElse(0L);
    this.unsignedContent = new ComponentHolder(version, decoratedMessage);
    this.senderName = new ComponentHolder(version, senderName);
  }

  public String getMessage() {
    return message;
  }

  public Component getUnsignedContent() {
    return unsignedContent.getComponent();
  }

  public byte[] getSignature() {
    return signature == null ? new byte[0] : signature.clone();
  }

  @Override
  public void decode(ByteBuf buf, ProtocolUtils.Direction direction, ProtocolVersion version) {
    if (version.noLessThan(ProtocolVersion.MINECRAFT_1_21_5)) {
      this.globalIndex = ProtocolUtils.readVarInt(buf);
    }
    this.sender = ProtocolUtils.readUuid(buf);
    this.index = ProtocolUtils.readVarInt(buf);
    if (buf.readBoolean()) {
      this.signature = readMessageSignature(buf);
    } else {
      this.signature = null;
    }
    this.message = ProtocolUtils.readString(buf, 256);
    this.timestamp = Instant.ofEpochMilli(buf.readLong());
    this.salt = buf.readLong();
    readPackedLastSeenMessages(buf);
    if (buf.readBoolean()) {
      this.unsignedContent = ComponentHolder.read(buf, version);
    } else {
      this.unsignedContent = null;
    }
    readFilterMask(buf);
    readChatTypeBound(buf, version);
  }

  @Override
  public void encode(ByteBuf buf, ProtocolUtils.Direction direction, ProtocolVersion version) {
    if (!supportsProtocol(version)) {
      throw new IllegalArgumentException("Clientbound player chat is not supported for " + version);
    }
    if (version.noLessThan(ProtocolVersion.MINECRAFT_1_21_5)) {
      ProtocolUtils.writeVarInt(buf, globalIndex);
    }
    ProtocolUtils.writeUuid(buf, sender);
    ProtocolUtils.writeVarInt(buf, index);
    buf.writeBoolean(signature != null);
    if (signature != null) {
      buf.writeBytes(signature);
    }
    ProtocolUtils.writeString(buf, message);
    buf.writeLong(timestamp.toEpochMilli());
    buf.writeLong(salt);
    writeEmptyPackedLastSeenMessages(buf);
    buf.writeBoolean(unsignedContent != null);
    if (unsignedContent != null) {
      unsignedContent.write(buf);
    }
    writePassThroughFilterMask(buf);
    writeChatTypeBound(buf);
  }

  @Override
  public boolean handle(MinecraftSessionHandler handler) {
    return handler.handle(this);
  }

  private static void readPackedLastSeenMessages(ByteBuf buf) {
    int entries = ProtocolUtils.readVarInt(buf);
    if (entries < 0 || entries > LastSeenMessages.WINDOW_SIZE) {
      throw new IllegalArgumentException("Invalid packed last-seen message count: " + entries);
    }
    for (int i = 0; i < entries; i++) {
      int id = ProtocolUtils.readVarInt(buf) - 1;
      if (id == -1) {
        readMessageSignature(buf);
      }
    }
  }

  private static byte[] readMessageSignature(ByteBuf buf) {
    byte[] signature = new byte[256];
    buf.readBytes(signature);
    return signature;
  }

  private static void writeEmptyPackedLastSeenMessages(ByteBuf buf) {
    ProtocolUtils.writeVarInt(buf, 0);
  }

  private static void readFilterMask(ByteBuf buf) {
    int maskType = ProtocolUtils.readVarInt(buf);
    if (maskType == 2) {
      int longs = ProtocolUtils.readVarInt(buf);
      buf.skipBytes(longs * Long.BYTES);
    }
  }

  private static void writePassThroughFilterMask(ByteBuf buf) {
    ProtocolUtils.writeVarInt(buf, 0);
  }

  private void readChatTypeBound(ByteBuf buf, ProtocolVersion version) {
    int holder = ProtocolUtils.readVarInt(buf);
    if (holder == 0) {
      throw new UnsupportedOperationException("Custom chat type decoding is not implemented");
    }
    this.senderName = ComponentHolder.read(buf, version);
    if (buf.readBoolean()) {
      ComponentHolder.read(buf, version);
    }
  }

  private void writeChatTypeBound(ByteBuf buf) {
    ProtocolUtils.writeVarInt(buf, CHAT_TYPE_CHAT_HOLDER_ID);
    senderName.write(buf);
    buf.writeBoolean(false);
  }

  public static boolean supportsProtocol(ProtocolVersion version) {
    return version.noLessThan(ProtocolVersion.MINECRAFT_1_19_3);
  }
}
