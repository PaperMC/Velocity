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

package com.velocitypowered.proxy.protocol.packet;

import static com.velocitypowered.proxy.connection.VelocityConstants.EMPTY_BYTE_ARRAY;

import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.proxy.connection.MinecraftSessionHandler;
import com.velocitypowered.proxy.protocol.MinecraftPacket;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import com.velocitypowered.proxy.protocol.ProtocolUtils.Direction;
import com.velocitypowered.proxy.util.except.QuietDecoderException;
import io.netty.buffer.ByteBuf;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Arrays;

public class EncryptionResponse implements MinecraftPacket {

  private final static QuietDecoderException NO_SALT = new QuietDecoderException(
      "Encryption response didn't contain salt");

  private byte[] sharedSecret = EMPTY_BYTE_ARRAY;
  private byte[] verifyToken = EMPTY_BYTE_ARRAY;
  private @Nullable Long salt;

  public byte[] getSharedSecret() {
    return sharedSecret.clone();
  }

  public byte[] getVerifyToken() {
    return verifyToken.clone();
  }

  public long getSalt() {
    if (salt == null) {
      throw NO_SALT;
    }
    return salt;
  }

  @Override
  public String toString() {
    return "EncryptionResponse{"
        + "sharedSecret=" + Arrays.toString(sharedSecret)
        + ", verifyToken=" + Arrays.toString(verifyToken)
        + '}';
  }

  @Override
  public void decode(ByteBuf buf, ProtocolUtils.Direction direction, ProtocolVersion version) {
    if (version.compareTo(ProtocolVersion.MINECRAFT_1_8) >= 0) {
      this.sharedSecret = ProtocolUtils.readByteArray(buf, 128);

      if (version.compareTo(ProtocolVersion.MINECRAFT_1_19) >= 0
          && version.compareTo(ProtocolVersion.MINECRAFT_1_19_3) < 0
          && !buf.readBoolean()) {
        salt = buf.readLong();
      }

      this.verifyToken = ProtocolUtils.readByteArray(buf,
          version.compareTo(ProtocolVersion.MINECRAFT_1_19) >= 0 ? 256 : 128);
    } else {
      this.sharedSecret = ProtocolUtils.readByteArray17(buf);
      this.verifyToken = ProtocolUtils.readByteArray17(buf);
    }
  }

  @Override
  public void encode(ByteBuf buf, ProtocolUtils.Direction direction, ProtocolVersion version) {
    if (version.compareTo(ProtocolVersion.MINECRAFT_1_8) >= 0) {
      ProtocolUtils.writeByteArray(buf, sharedSecret);
      if (version.compareTo(ProtocolVersion.MINECRAFT_1_19) >= 0
          && version.compareTo(ProtocolVersion.MINECRAFT_1_19_3) < 0) {
        if (salt != null) {
          buf.writeBoolean(false);
          buf.writeLong(salt);
        } else {
          buf.writeBoolean(true);
        }
      }
      ProtocolUtils.writeByteArray(buf, verifyToken);
    } else {
      ProtocolUtils.writeByteArray17(sharedSecret, buf, false);
      ProtocolUtils.writeByteArray17(verifyToken, buf, false);
    }
  }

  @Override
  public boolean handle(MinecraftSessionHandler handler) {
    return handler.handle(this);
  }

  @Override
  public int expectedMaxLength(ByteBuf buf, Direction direction, ProtocolVersion version) {
    // It turns out these come out to the same length, whether we're talking >=1.8 or not.
    // The length prefix always winds up being 2 bytes.
    int base = 256 + 2 + 2;
    if (version.compareTo(ProtocolVersion.MINECRAFT_1_19_3) >= 0) {
      return base + 128;
    }
    if (version.compareTo(ProtocolVersion.MINECRAFT_1_19) >= 0) {
      // Verify token is twice as long on 1.19+
      // Additional 1 byte for left <> right and 8 bytes for salt
      base += 128 + 8 + 1;
    }
    return base;
  }

  @Override
  public int expectedMinLength(ByteBuf buf, Direction direction, ProtocolVersion version) {
    int base = expectedMaxLength(buf, direction, version);
    if (version.compareTo(ProtocolVersion.MINECRAFT_1_19) >= 0) {
      // These are "optional"
      base -= 128 + 8;
    }
    return base;
  }
}
