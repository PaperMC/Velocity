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

package com.velocitypowered.proxy.protocol.packet.config;

import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.proxy.connection.MinecraftSessionHandler;
import com.velocitypowered.proxy.protocol.MinecraftPacket;
import com.velocitypowered.proxy.protocol.PacketCodec;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import com.velocitypowered.proxy.protocol.ProtocolUtils.Direction;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.DefaultByteBufHolder;

public final class RegistrySyncPacket extends DefaultByteBufHolder implements MinecraftPacket {

  public RegistrySyncPacket(ByteBuf backing) {
    super(backing);
  }

  @Override
  public boolean handle(MinecraftSessionHandler handler) {
    return handler.handle(this);
  }

  public int encodeSizeHint(Direction direction, ProtocolVersion version) {
    return content().readableBytes();
  }

  public static class Codec implements PacketCodec<RegistrySyncPacket> {
    public static final Codec INSTANCE = new Codec();

    @Override
    public RegistrySyncPacket decode(ByteBuf buf, ProtocolUtils.Direction direction,
        ProtocolVersion protocolVersion) {
      // NBT change in 1.20.2 makes it difficult to parse this packet.
      return new RegistrySyncPacket(buf.readRetainedSlice(buf.readableBytes()));
    }

    @Override
    public void encode(RegistrySyncPacket packet, ByteBuf buf,
        ProtocolUtils.Direction direction, ProtocolVersion protocolVersion) {
      buf.writeBytes(packet.content());
    }
  }
}
