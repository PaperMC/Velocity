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

public final class EncryptionRequestPacket implements MinecraftPacket {

  private final String serverId;
  private final byte[] publicKey;
  private final byte[] verifyToken;
  private final boolean shouldAuthenticate;

  public EncryptionRequestPacket(String serverId, byte[] publicKey, byte[] verifyToken,
                                  boolean shouldAuthenticate) {
    this.serverId = serverId;
    this.publicKey = publicKey.clone();
    this.verifyToken = verifyToken.clone();
    this.shouldAuthenticate = shouldAuthenticate;
  }

  public String serverId() {
    return serverId;
  }

  public byte[] publicKey() {
    return publicKey.clone();
  }

  public byte[] verifyToken() {
    return verifyToken.clone();
  }

  public boolean shouldAuthenticate() {
    return shouldAuthenticate;
  }

  @Override
  public boolean handle(MinecraftSessionHandler handler) {
    return handler.handle(this);
  }

  public static class Codec implements PacketCodec<EncryptionRequestPacket> {
    public static final Codec INSTANCE = new Codec();

    @Override
    public EncryptionRequestPacket decode(ByteBuf buf, ProtocolUtils.Direction direction,
                                           ProtocolVersion version) {
      String serverId = ProtocolUtils.readString(buf, 20);
      byte[] publicKey;
      byte[] verifyToken;
      boolean shouldAuthenticate = true;

      if (version.noLessThan(ProtocolVersion.MINECRAFT_1_8)) {
        publicKey = ProtocolUtils.readByteArray(buf, 256);
        verifyToken = ProtocolUtils.readByteArray(buf, 16);
        if (version.noLessThan(ProtocolVersion.MINECRAFT_1_20_5)) {
          shouldAuthenticate = buf.readBoolean();
        }
      } else {
        publicKey = ProtocolUtils.readByteArray17(buf);
        verifyToken = ProtocolUtils.readByteArray17(buf);
      }

      return new EncryptionRequestPacket(serverId, publicKey, verifyToken, shouldAuthenticate);
    }

    @Override
    public void encode(EncryptionRequestPacket packet, ByteBuf buf, ProtocolUtils.Direction direction,
                       ProtocolVersion version) {
      ProtocolUtils.writeString(buf, packet.serverId);

      if (version.noLessThan(ProtocolVersion.MINECRAFT_1_8)) {
        ProtocolUtils.writeByteArray(buf, packet.publicKey);
        ProtocolUtils.writeByteArray(buf, packet.verifyToken);
        if (version.noLessThan(ProtocolVersion.MINECRAFT_1_20_5)) {
          buf.writeBoolean(packet.shouldAuthenticate);
        }
      } else {
        ProtocolUtils.writeByteArray17(packet.publicKey, buf, false);
        ProtocolUtils.writeByteArray17(packet.verifyToken, buf, false);
      }
    }
  }
}
