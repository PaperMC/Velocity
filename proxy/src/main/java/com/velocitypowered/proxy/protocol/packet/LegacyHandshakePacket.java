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
import io.netty.buffer.ByteBuf;

public final class LegacyHandshakePacket implements MinecraftPacket {

  public static final LegacyHandshakePacket INSTANCE = new LegacyHandshakePacket();

  private LegacyHandshakePacket() {
  }

  @Override
  public boolean handle(MinecraftSessionHandler handler) {
    return handler.handle(this);
  }

  public static class Codec implements PacketCodec<LegacyHandshakePacket> {
    public static final Codec INSTANCE = new Codec();

    @Override
    public LegacyHandshakePacket decode(ByteBuf buf, ProtocolUtils.Direction direction,
                                         ProtocolVersion version) {
      throw new UnsupportedOperationException();
    }

    @Override
    public void encode(LegacyHandshakePacket packet, ByteBuf buf, ProtocolUtils.Direction direction,
                       ProtocolVersion version) {
      throw new UnsupportedOperationException();
    }
  }
}
