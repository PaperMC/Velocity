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

package com.velocitypowered.proxy.network.packet.serverbound;

import com.google.common.base.MoreObjects;
import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.proxy.network.ProtocolUtils;
import com.velocitypowered.proxy.network.packet.Packet;
import com.velocitypowered.proxy.network.packet.PacketHandler;
import com.velocitypowered.proxy.network.packet.PacketReader;
import com.velocitypowered.proxy.network.packet.PacketWriter;
import io.netty.buffer.ByteBuf;

public class ServerboundEncryptionResponsePacket implements Packet {
  public static final PacketReader<ServerboundEncryptionResponsePacket> DECODER = new PacketReader<>() {
    @Override
    public ServerboundEncryptionResponsePacket read(ByteBuf buf,
        ProtocolVersion version) {
      final byte[] sharedSecret;
      final byte[] verifyToken;
      if (version.gte(ProtocolVersion.MINECRAFT_1_8)) {
        sharedSecret = ProtocolUtils.readByteArray(buf, 128);
        verifyToken = ProtocolUtils.readByteArray(buf, 128);
      } else {
        sharedSecret = ProtocolUtils.readByteArray17(buf, 128);
        verifyToken = ProtocolUtils.readByteArray17(buf, 128);
      }
      return new ServerboundEncryptionResponsePacket(sharedSecret, verifyToken);
    }

    @Override
    public int expectedMaxLength(ByteBuf buf, ProtocolVersion version) {
      // Both arrays are always 128 bytes long (due to padding), and each array has 2 bytes of
      // padding (which applies to 1.7 since the length is written as a short and applies to 1.8+
      // as 128 encodes as a 2-byte VarInt).
      return 260;
    }

    @Override
    public int expectedMinLength(ByteBuf buf, ProtocolVersion version) {
      return expectedMaxLength(buf, version);
    }
  };
  public static final PacketWriter<ServerboundEncryptionResponsePacket> ENCODER = PacketWriter.deprecatedEncode();

  private final byte[] sharedSecret;
  private final byte[] verifyToken;

  private ServerboundEncryptionResponsePacket(final byte[] sharedSecret, final byte[] verifyToken) {
    this.sharedSecret = sharedSecret;
    this.verifyToken = verifyToken;
  }

  @Override
  public void encode(ByteBuf buf, ProtocolVersion version) {
    if (version.gte(ProtocolVersion.MINECRAFT_1_8)) {
      ProtocolUtils.writeByteArray(buf, sharedSecret);
      ProtocolUtils.writeByteArray(buf, verifyToken);
    } else {
      ProtocolUtils.writeByteArray17(sharedSecret, buf, false);
      ProtocolUtils.writeByteArray17(verifyToken, buf, false);
    }
  }

  @Override
  public boolean handle(PacketHandler handler) {
    return handler.handle(this);
  }

  public byte[] getSharedSecret() {
    return sharedSecret.clone();
  }

  public byte[] getVerifyToken() {
    return verifyToken.clone();
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
      .add("sharedSecret", this.sharedSecret)
      .add("verifyToken", this.verifyToken)
      .toString();
  }
}
