/*
 * Copyright (C) 2026 Velocity Contributors
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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.proxy.crypto.SessionKeyExchange;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import com.velocitypowered.proxy.protocol.ProtocolUtils.Direction;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.security.SecureRandom;
import org.junit.jupiter.api.Test;

class EncryptionPacketTest {

  private static final ProtocolVersion VERSION = ProtocolVersion.MINECRAFT_26_4;

  @Test
  void requestRoundTripsOn264() {
    byte[] publicKey = SessionKeyExchange.generateKeyPair(new SecureRandom())
        .getPublic().getEncoded();
    EncryptionRequestPacket request = new EncryptionRequestPacket();
    request.setServerId("server");
    request.setPublicKey(publicKey);
    request.setSalt(-1234567890123L);

    ByteBuf buf = Unpooled.buffer();
    try {
      request.encode(buf, Direction.CLIENTBOUND, VERSION);

      // salt, server id, public key, authenticate - and no verify token
      ByteBuf expected = Unpooled.buffer();
      try {
        expected.writeLong(-1234567890123L);
        ProtocolUtils.writeString(expected, "server");
        ProtocolUtils.writeByteArray(expected, publicKey);
        expected.writeBoolean(true);
        assertEquals(expected, buf);
      } finally {
        expected.release();
      }

      EncryptionRequestPacket decoded = new EncryptionRequestPacket();
      decoded.decode(buf, Direction.CLIENTBOUND, VERSION);
      assertFalse(buf.isReadable(), "trailing bytes");
      assertEquals("server", decoded.getServerId());
      assertArrayEquals(publicKey, decoded.getPublicKey());
      assertEquals(-1234567890123L, decoded.getSalt());
      assertEquals(0, decoded.getVerifyToken().length);
    } finally {
      buf.release();
    }
  }

  @Test
  void responseRoundTripsOn264() {
    byte[] publicKey = SessionKeyExchange.generateKeyPair(new SecureRandom())
        .getPublic().getEncoded();
    byte[] signature = new byte[32];
    for (int i = 0; i < signature.length; i++) {
      signature[i] = (byte) i;
    }

    ByteBuf buf = Unpooled.buffer();
    try {
      ProtocolUtils.writeByteArray(buf, publicKey);
      buf.writeBytes(signature);
      int length = buf.readableBytes();

      EncryptionResponsePacket response = new EncryptionResponsePacket();
      assertTrue(length >= response.decodeExpectedMinLength(buf, Direction.SERVERBOUND, VERSION));
      assertTrue(length <= response.decodeExpectedMaxLength(buf, Direction.SERVERBOUND, VERSION));
      response.decode(buf, Direction.SERVERBOUND, VERSION);
      assertFalse(buf.isReadable(), "trailing bytes");
      assertArrayEquals(publicKey, response.getPublicKey());
      assertArrayEquals(signature, response.getSignature());

      ByteBuf encoded = Unpooled.buffer();
      try {
        response.encode(encoded, Direction.SERVERBOUND, VERSION);
        buf.readerIndex(0);
        assertEquals(buf, encoded);
      } finally {
        encoded.release();
      }
    } finally {
      buf.release();
    }
  }
}
