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

import static com.velocitypowered.api.network.ProtocolVersion.MINECRAFT_1_7_6;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.velocitypowered.proxy.protocol.ProtocolUtils;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link JoinGamePacket} dimension handling for pre-1.9.1 versions
 * where modded servers may use unsigned byte dimension IDs (e.g. GTNH).
 */
class JoinGamePacketTest {

  /**
   * Vanilla 1.7.10 uses a signed byte for dimension: -1 (Nether), 0 (Overworld),
   * 1 (End). Verifies that the standard vanilla values round-trip correctly.
   */
  @Test
  void decodeLegacyVanillaDimensions() {
    assertLegacyDimensionDecoded((byte) -1, -1);
    assertLegacyDimensionDecoded((byte) 0, 0);
    assertLegacyDimensionDecoded((byte) 1, 1);
  }

  /**
   * Modded servers like GTNH repurpose the dimension byte as unsigned to support
   * dimension IDs up to 255. A byte value of 0xB4 should be interpreted as 180,
   * not sign-extended to -76, because {@link RespawnPacket#fromJoinGame(JoinGamePacket)}
   * copies the dimension into a packet that encodes it as a 4-byte int for pre-1.16
   * versions. Sending -76 as an int crashes the client with an illegal dimension ID.
   */
  @Test
  void decodeLegacyUnsignedModdedDimensions() {
    // 0xB4 as signed byte = -76, as unsigned = 180
    assertLegacyDimensionDecoded((byte) 0xB4, 180);
    // 0x80 as signed byte = -128, as unsigned = 128
    assertLegacyDimensionDecoded((byte) 0x80, 128);
    // 0xFE as signed byte = -2, as unsigned = 254
    assertLegacyDimensionDecoded((byte) 0xFE, 254);
  }

  /**
   * 0xFF is ambiguous: it could be vanilla Nether (-1) or modded dimension 255.
   * We resolve in Nether's favor since vanilla 1.7.10 only ever uses -1/0/1.
   */
  @Test
  void decodeLegacyAmbiguousByte0xFF() {
    // 0xFF → vanilla Nether (-1) takes priority over modded dim 255
    assertLegacyDimensionDecoded((byte) 0xFF, -1);
  }

  /**
   * Verifies that RespawnPacket.fromJoinGame copies the canonical dimension.
   * The actual bug manifests in RespawnPacket because it writes dimension as a
   * 4-byte int for pre-1.16 versions, unlike JoinGamePacket which writes a byte.
   */
  @Test
  void fromJoinGameCopiesCanonicalDimension() {
    JoinGamePacket joinGame = new JoinGamePacket();
    joinGame.setDimension(180);

    RespawnPacket respawn = RespawnPacket.fromJoinGame(joinGame);
    assertEquals(180, respawn.getDimension(),
        "RespawnPacket must carry the canonical dimension for correct int encoding");
  }

  /**
   * Helper: encodes a legacy JoinGame packet with the given dimension byte,
   * decodes it for a pre-1.9.1 version, and asserts the stored dimension value.
   */
  private static void assertLegacyDimensionDecoded(byte wireByte, int expectedDimension) {
    ByteBuf buf = Unpooled.buffer();
    try {
      // Write a minimal 1.7.6-1.7.10 JoinGame packet.
      // Fields read by decodeLegacy for MINECRAFT_1_7_6:
      //   entityId(int), gamemode(byte), dimension(byte),
      //   difficulty(byte, because <=1.13.2), maxPlayers(byte), levelType(string)
      // Fields NOT read for 1.7.6: partialHashedSeed (1.15+), viewDistance (1.14+),
      //   reducedDebugInfo (1.8+), showRespawnScreen (1.15+)
      buf.writeInt(0);          // entityId
      buf.writeByte(0);         // gamemode (0 = survival, no hardcore bit)
      buf.writeByte(wireByte);  // dimension
      buf.writeByte(1);         // difficulty (1 = easy)
      buf.writeByte(20);        // maxPlayers
      ProtocolUtils.writeString(buf, "default"); // levelType

      JoinGamePacket packet = new JoinGamePacket();
      packet.decode(buf, ProtocolUtils.Direction.CLIENTBOUND, MINECRAFT_1_7_6);

      assertEquals(expectedDimension, packet.getDimension(),
          "Dimension byte 0x" + Integer.toHexString(Byte.toUnsignedInt(wireByte))
              + " should decode to " + expectedDimension);
    } finally {
      buf.release();
    }
  }
}
