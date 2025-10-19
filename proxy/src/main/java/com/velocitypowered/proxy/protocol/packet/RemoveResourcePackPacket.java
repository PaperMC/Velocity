/*
 * Copyright (C) 2018-2021 Velocity Contributors
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

package com.velocitypowered.proxy.protocol.packet;

import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.proxy.connection.MinecraftSessionHandler;
import com.velocitypowered.proxy.protocol.MinecraftPacket;
import com.velocitypowered.proxy.protocol.PacketCodec;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import com.velocitypowered.proxy.protocol.ProtocolUtils.Direction;
import io.netty.buffer.ByteBuf;
import java.util.UUID;
import org.checkerframework.checker.nullness.qual.Nullable;

public record RemoveResourcePackPacket(@Nullable UUID id) implements MinecraftPacket {

  public @Nullable UUID getId() {
    return id;
  }

  @Override
  public boolean handle(MinecraftSessionHandler handler) {
    return handler.handle(this);
  }

  public static class Codec implements PacketCodec<RemoveResourcePackPacket> {
    public static final Codec INSTANCE = new Codec();

    @Override
    public RemoveResourcePackPacket decode(ByteBuf buf, Direction direction,
        ProtocolVersion protocolVersion) {
      UUID id = null;
      if (buf.readBoolean()) {
        id = ProtocolUtils.readUuid(buf);
      }
      return new RemoveResourcePackPacket(id);
    }

    @Override
    public void encode(RemoveResourcePackPacket packet, ByteBuf buf, Direction direction,
        ProtocolVersion protocolVersion) {
      buf.writeBoolean(packet.id != null);
      if (packet.id != null) {
        ProtocolUtils.writeUuid(buf, packet.id);
      }
    }
  }
}
