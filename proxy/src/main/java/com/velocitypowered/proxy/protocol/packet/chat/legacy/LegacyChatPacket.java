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

package com.velocitypowered.proxy.protocol.packet.chat.legacy;

import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.proxy.connection.MinecraftSessionHandler;
import com.velocitypowered.proxy.protocol.MinecraftPacket;
import com.velocitypowered.proxy.protocol.PacketCodec;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import io.netty.buffer.ByteBuf;
import java.util.UUID;
import org.checkerframework.checker.nullness.qual.Nullable;

public record LegacyChatPacket(String message, byte type, @Nullable UUID sender) implements MinecraftPacket {

  public static final byte CHAT_TYPE = (byte) 0;
  public static final byte SYSTEM_TYPE = (byte) 1;
  public static final byte GAME_INFO_TYPE = (byte) 2;

  public static final int MAX_SERVERBOUND_MESSAGE_LENGTH = 256;
  public static final UUID EMPTY_SENDER = new UUID(0, 0);

  public String getMessage() {
    return message;
  }

  @Override
  public boolean handle(MinecraftSessionHandler handler) {
    return handler.handle(this);
  }

  public static class Codec implements PacketCodec<LegacyChatPacket> {
    @Override
    public LegacyChatPacket decode(ByteBuf buf, ProtocolUtils.Direction direction,
                                    ProtocolVersion version) {
      String message = ProtocolUtils.readString(buf, direction == ProtocolUtils.Direction.CLIENTBOUND
          ? 262144 : version.noLessThan(ProtocolVersion.MINECRAFT_1_11) ? 256 : 100);
      byte type = 0;
      UUID sender = null;
      if (direction == ProtocolUtils.Direction.CLIENTBOUND
          && version.noLessThan(ProtocolVersion.MINECRAFT_1_8)) {
        type = buf.readByte();
        if (version.noLessThan(ProtocolVersion.MINECRAFT_1_16)) {
          sender = ProtocolUtils.readUuid(buf);
        }
      }
      return new LegacyChatPacket(message, type, sender);
    }

    @Override
    public void encode(LegacyChatPacket packet, ByteBuf buf, ProtocolUtils.Direction direction,
                       ProtocolVersion version) {
      ProtocolUtils.writeString(buf, packet.message);
      if (direction == ProtocolUtils.Direction.CLIENTBOUND
          && version.noLessThan(ProtocolVersion.MINECRAFT_1_8)) {
        buf.writeByte(packet.type);
        if (version.noLessThan(ProtocolVersion.MINECRAFT_1_16)) {
          ProtocolUtils.writeUuid(buf, packet.sender == null ? EMPTY_SENDER : packet.sender);
        }
      }
    }
  }
}
