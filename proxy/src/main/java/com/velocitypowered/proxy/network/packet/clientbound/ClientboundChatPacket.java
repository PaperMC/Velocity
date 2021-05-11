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

package com.velocitypowered.proxy.network.packet.clientbound;

import com.google.common.base.MoreObjects;
import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.proxy.network.ProtocolUtils;
import com.velocitypowered.proxy.network.packet.Packet;
import com.velocitypowered.proxy.network.packet.PacketDirection;
import com.velocitypowered.proxy.network.packet.PacketHandler;
import com.velocitypowered.proxy.network.packet.PacketReader;
import com.velocitypowered.proxy.network.packet.PacketWriter;
import io.netty.buffer.ByteBuf;
import java.util.UUID;
import org.checkerframework.checker.nullness.qual.Nullable;

public class ClientboundChatPacket implements Packet {
  public static final PacketReader<ClientboundChatPacket> DECODER = PacketReader.method(ClientboundChatPacket::new);
  public static final PacketWriter<ClientboundChatPacket> ENCODER = PacketWriter.deprecatedEncode();

  public static final byte CHAT_TYPE = (byte) 0;
  public static final byte SYSTEM_TYPE = (byte) 1;
  public static final byte GAME_INFO_TYPE = (byte) 2;

  private @Nullable String message;
  private byte type;
  private @Nullable UUID sender;

  private ClientboundChatPacket() {
  }

  public ClientboundChatPacket(String message, byte type, UUID sender) {
    this.message = message;
    this.type = type;
    this.sender = sender;
  }

  @Override
  public void decode(ByteBuf buf, PacketDirection direction, ProtocolVersion version) {
    message = ProtocolUtils.readString(buf);
    if (version.gte(ProtocolVersion.MINECRAFT_1_8)) {
      type = buf.readByte();
      if (version.gte(ProtocolVersion.MINECRAFT_1_16)) {
        sender = ProtocolUtils.readUuid(buf);
      }
    }
  }

  @Override
  public void encode(ByteBuf buf, ProtocolVersion version) {
    if (message == null) {
      throw new IllegalStateException("Message is not specified");
    }
    ProtocolUtils.writeString(buf, message);
    if (version.gte(ProtocolVersion.MINECRAFT_1_8)) {
      buf.writeByte(type);
      if (version.compareTo(ProtocolVersion.MINECRAFT_1_16) >= 0) {
        ProtocolUtils.writeUuid(buf, sender);
      }
    }
  }

  @Override
  public boolean handle(PacketHandler handler) {
    return handler.handle(this);
  }

  public String getMessage() {
    if (message == null) {
      throw new IllegalStateException("Message is not specified");
    }
    return message;
  }

  public byte getType() {
    return type;
  }

  public UUID getSenderUuid() {
    return sender;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
      .add("message", this.message)
      .add("type", this.type)
      .add("sender", this.sender)
      .toString();
  }
}
