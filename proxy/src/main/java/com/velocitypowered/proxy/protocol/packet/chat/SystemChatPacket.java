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

package com.velocitypowered.proxy.protocol.packet.chat;

import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.proxy.connection.MinecraftSessionHandler;
import com.velocitypowered.proxy.protocol.MinecraftPacket;
import com.velocitypowered.proxy.protocol.PacketCodec;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import io.netty.buffer.ByteBuf;

public final class SystemChatPacket implements MinecraftPacket {

  private final ComponentHolder component;
  private final ChatType type;

  public SystemChatPacket() {
    this(null, ChatType.SYSTEM);
  }

  public SystemChatPacket(ComponentHolder component, ChatType type) {
    this.component = component;
    this.type = type;
  }

  public ChatType type() {
    return type;
  }

  public ComponentHolder component() {
    return component;
  }

  public ComponentHolder getComponent() {
    return component();
  }

  public ChatType getType() {
    return type();
  }

  @Override
  public boolean handle(MinecraftSessionHandler handler) {
    return handler.handle(this);
  }

  public static class Codec implements PacketCodec<SystemChatPacket> {
    @Override
    public SystemChatPacket decode(ByteBuf buf, ProtocolUtils.Direction direction,
                                    ProtocolVersion version) {
      ComponentHolder component = ComponentHolder.read(buf, version);
      ChatType type;
      if (version.noLessThan(ProtocolVersion.MINECRAFT_1_19_1)) {
        type = buf.readBoolean() ? ChatType.GAME_INFO : ChatType.SYSTEM;
      } else {
        type = ChatType.values()[ProtocolUtils.readVarInt(buf)];
      }
      return new SystemChatPacket(component, type);
    }

    @Override
    public void encode(SystemChatPacket packet, ByteBuf buf, ProtocolUtils.Direction direction,
                       ProtocolVersion version) {
      packet.component.write(buf);
      if (version.noLessThan(ProtocolVersion.MINECRAFT_1_19_1)) {
        switch (packet.type) {
          case SYSTEM:
            buf.writeBoolean(false);
            break;
          case GAME_INFO:
            buf.writeBoolean(true);
            break;
          default:
            throw new IllegalArgumentException("Invalid chat type");
        }
      } else {
        ProtocolUtils.writeVarInt(buf, packet.type.getId());
      }
    }
  }
}
