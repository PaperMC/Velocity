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

import static com.velocitypowered.proxy.connection.forge.legacy.LegacyForgeConstants.HANDSHAKE_HOSTNAME_TOKEN;

import com.velocitypowered.api.network.HandshakeIntent;
import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.proxy.connection.MinecraftSessionHandler;
import com.velocitypowered.proxy.protocol.MinecraftPacket;
import com.velocitypowered.proxy.protocol.PacketCodec;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import io.netty.buffer.ByteBuf;

public record HandshakePacket(ProtocolVersion protocolVersion, String serverAddress, int port,
                               HandshakeIntent intent)
    implements MinecraftPacket {

  // This size was chosen to ensure Forge clients can still connect even with very long hostnames.
  // While DNS technically allows any character to be used, in practice ASCII is used.
  private static final int MAXIMUM_HOSTNAME_LENGTH = 255 + HANDSHAKE_HOSTNAME_TOKEN.length() + 1;

  public static final HandshakePacket DEFAULT = new HandshakePacket(ProtocolVersion.MINIMUM_VERSION, "", 0,
      HandshakeIntent.LOGIN);

  public int nextStatus() {
    return intent.id();
  }

  @Override
  public boolean handle(MinecraftSessionHandler handler) {
    return handler.handle(this);
  }

  public static class Codec implements PacketCodec<HandshakePacket> {
    public static final Codec INSTANCE = new Codec();

    @Override
    public HandshakePacket decode(ByteBuf buf, ProtocolUtils.Direction direction,
                                   ProtocolVersion ignored) {
      int realProtocolVersion = ProtocolUtils.readVarInt(buf);
      ProtocolVersion protocolVersion = ProtocolVersion.getProtocolVersion(realProtocolVersion);
      String serverAddress = ProtocolUtils.readString(buf, MAXIMUM_HOSTNAME_LENGTH);
      int port = buf.readUnsignedShort();
      int nextStatus = ProtocolUtils.readVarInt(buf);
      HandshakeIntent intent = HandshakeIntent.getById(nextStatus);
      return new HandshakePacket(protocolVersion, serverAddress, port, intent);
    }

    @Override
    public void encode(HandshakePacket packet, ByteBuf buf, ProtocolUtils.Direction direction,
                       ProtocolVersion ignored) {
      ProtocolUtils.writeVarInt(buf, packet.protocolVersion().getProtocol());
      ProtocolUtils.writeString(buf, packet.serverAddress());
      buf.writeShort(packet.port());
      ProtocolUtils.writeVarInt(buf, packet.nextStatus());
    }

    @Override
    public int decodeExpectedMinLength(ByteBuf buf, ProtocolUtils.Direction direction,
        ProtocolVersion version) {
      return 7;
    }

    @Override
    public int decodeExpectedMaxLength(ByteBuf buf, ProtocolUtils.Direction direction,
        ProtocolVersion version) {
      return 9 + (MAXIMUM_HOSTNAME_LENGTH * 3);
    }

    @Override
    public int encodeSizeHint(HandshakePacket packet, ProtocolUtils.Direction direction, ProtocolVersion version) {
      // We could compute an exact size, but 4KiB ought to be enough to encode all reasonable
      // sizes of this packet.
      return 4 * 1024;
    }
  }
}
