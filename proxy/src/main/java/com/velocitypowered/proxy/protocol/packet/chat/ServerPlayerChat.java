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
import java.time.Instant;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import org.checkerframework.checker.nullness.qual.Nullable;

public class ServerPlayerChat implements MinecraftPacket {

  private Component component;
  private @Nullable Component unsignedComponent;
  private int type;

  private UUID sender;
  private Component senderName;
  private @Nullable Component teamName;

  private Instant expiry;

  public void setType(int type) {
    this.type = type;
  }

  public void setComponent(Component component) {
    this.component = component;
  }

  public int getType() {
    return type;
  }

  public Component getComponent() {
    return component;
  }

  @Override
  public void decode(ByteBuf buf, ProtocolUtils.Direction direction, ProtocolVersion protocolVersion) {
    component = ProtocolUtils.getJsonChatSerializer(protocolVersion).deserialize(ProtocolUtils.readString(buf));
    if (buf.readBoolean()) {
      unsignedComponent = component = ProtocolUtils.getJsonChatSerializer(protocolVersion)
          .deserialize(ProtocolUtils.readString(buf));
    }

    type = ProtocolUtils.readVarInt(buf);

    sender = ProtocolUtils.readUuid(buf);
    senderName = ProtocolUtils.getJsonChatSerializer(protocolVersion).deserialize(ProtocolUtils.readString(buf));
    if (buf.readBoolean()) {
      teamName = ProtocolUtils.getJsonChatSerializer(protocolVersion).deserialize(ProtocolUtils.readString(buf));
    }

    expiry = Instant.ofEpochMilli(buf.readLong());

    long salt = buf.readLong();
    byte[] signature = ProtocolUtils.readByteArray(buf);
  }

  @Override
  public void encode(ByteBuf buf, ProtocolUtils.Direction direction, ProtocolVersion protocolVersion) {
    // TBD
  }

  @Override
  public boolean handle(MinecraftSessionHandler handler) {
    return handler.handle(this);
  }
}
