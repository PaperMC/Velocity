/*
 * Copyright (C) 2024 Velocity Contributors
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
import java.net.InetSocketAddress;
import org.jetbrains.annotations.Nullable;

public record TransferPacket(String host, int port) implements MinecraftPacket {

  @Nullable
  public InetSocketAddress address() {
    if (host == null) {
      return null;
    }
    return new InetSocketAddress(host, port);
  }

  public String getHost() {
    return host;
  }

  public int getPort() {
    return port;
  }

  @Override
  public boolean handle(MinecraftSessionHandler handler) {
    return handler.handle(this);
  }

  public static class Codec implements PacketCodec<TransferPacket> {
    public static final Codec INSTANCE = new Codec();

    @Override
    public TransferPacket decode(ByteBuf buf, ProtocolUtils.Direction direction,
        ProtocolVersion protocolVersion) {
      return new TransferPacket(ProtocolUtils.readString(buf), ProtocolUtils.readVarInt(buf));
    }

    @Override
    public void encode(TransferPacket packet, ByteBuf buf, ProtocolUtils.Direction direction,
        ProtocolVersion protocolVersion) {
      ProtocolUtils.writeString(buf, packet.host);
      ProtocolUtils.writeVarInt(buf, packet.port);
    }
  }
}
