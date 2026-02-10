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
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import io.netty.buffer.ByteBuf;

/**
 * Represents a packet sent from the server to the client to display system chat messages.
 * This packet handles the communication of messages that are not player-generated, but instead
 * come from the system or server itself.
 */
public class SystemChatPacket implements MinecraftPacket {

  public SystemChatPacket() {
  }

  public SystemChatPacket(ComponentHolder component, ChatType type) {
    this.component = component;
    this.type = type;
  }

  private ComponentHolder component;
  private ChatType type;

  public ChatType getType() {
    return type;
  }

  public ComponentHolder getComponent() {
    return component;
  }

  @Override
  public void decode(ByteBuf buf, ProtocolUtils.Direction direction, ProtocolVersion version) {
    component = ComponentHolder.read(buf, version);
    if (version.noLessThan(ProtocolVersion.MINECRAFT_1_19_1)) {
      type = buf.readBoolean() ? ChatType.GAME_INFO : ChatType.SYSTEM;
    } else {
      type = ChatType.values()[ProtocolUtils.readVarInt(buf)];
    }
  }

  @Override
  public void encode(ByteBuf buf, ProtocolUtils.Direction direction, ProtocolVersion version) {
    component.write(buf);
    if (version.noLessThan(ProtocolVersion.MINECRAFT_1_19_1)) {
      switch (type) {
        case SYSTEM -> buf.writeBoolean(false);
        case GAME_INFO -> buf.writeBoolean(true);
        default -> throw new IllegalArgumentException("Invalid chat type");
      }
    } else {
      ProtocolUtils.writeVarInt(buf, type.getId());
    }
  }

  @Override
  public boolean handle(MinecraftSessionHandler handler) {
    return handler.handle(this);
  }
}
