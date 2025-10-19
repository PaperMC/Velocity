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
import com.velocitypowered.proxy.protocol.ProtocolUtils.Direction;
import io.netty.buffer.ByteBuf;
import net.kyori.adventure.key.Key;
import org.checkerframework.checker.nullness.qual.Nullable;

public record ServerboundCookieResponsePacket(Key key,
    byte @Nullable [] payload) implements MinecraftPacket {

  @Override
  public boolean handle(MinecraftSessionHandler handler) {
    return handler.handle(this);
  }

  public static class Codec implements PacketCodec<ServerboundCookieResponsePacket> {
    public static final Codec INSTANCE = new Codec();

    @Override
    public ServerboundCookieResponsePacket decode(ByteBuf buf, Direction direction,
        ProtocolVersion protocolVersion) {
      Key key = ProtocolUtils.readKey(buf);
      byte[] payload = null;
      if (buf.readBoolean()) {
        payload = ProtocolUtils.readByteArray(buf, 5120);
      }
      return new ServerboundCookieResponsePacket(key, payload);
    }

    @Override
    public void encode(ServerboundCookieResponsePacket packet, ByteBuf buf, Direction direction,
        ProtocolVersion protocolVersion) {
      ProtocolUtils.writeKey(buf, packet.key);
      final boolean hasPayload = packet.payload != null && packet.payload.length > 0;
      buf.writeBoolean(hasPayload);
      if (hasPayload) {
        ProtocolUtils.writeByteArray(buf, packet.payload);
      }
    }
  }
}
