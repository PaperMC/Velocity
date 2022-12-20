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

package com.velocitypowered.proxy.protocol.packet.chat;

import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.proxy.connection.MinecraftSessionHandler;
import com.velocitypowered.proxy.protocol.MinecraftPacket;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import io.netty.buffer.ByteBuf;
import net.kyori.adventure.text.Component;

public class SystemChat implements MinecraftPacket {

  public SystemChat() {}

  public SystemChat(Component component, ChatType type) {
    this.component = component;
    this.type = type;
  }

  private Component component;
  private ChatType type;

  public ChatType getType() {
    return type;
  }

  public Component getComponent() {
    return component;
  }

  @Override
  public void decode(ByteBuf buf, ProtocolUtils.Direction direction, ProtocolVersion protocolVersion) {
    component = ProtocolUtils.getJsonChatSerializer(protocolVersion).deserialize(ProtocolUtils.readString(buf));
    // System chat is never decoded so this doesn't matter for now
    type = ChatType.values()[ProtocolUtils.readVarInt(buf)];
  }

  @Override
  public void encode(ByteBuf buf, ProtocolUtils.Direction direction, ProtocolVersion protocolVersion) {
    ProtocolUtils.writeString(buf, ProtocolUtils.getJsonChatSerializer(protocolVersion).serialize(component));
    if (protocolVersion.compareTo(ProtocolVersion.MINECRAFT_1_19_1) >= 0) {
      switch (type) {
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
      ProtocolUtils.writeVarInt(buf, type.getId());
    }
  }

  @Override
  public boolean handle(MinecraftSessionHandler handler) {
    return handler.handle(this);
  }
}
