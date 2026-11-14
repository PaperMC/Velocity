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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.Map;
import org.junit.jupiter.api.Test;

class TransferPacketTest {

  private static TransferPacket roundTrip(TransferPacket packet, ProtocolVersion version) {
    ByteBuf buf = Unpooled.buffer();
    try {
      packet.encode(buf, ProtocolUtils.Direction.CLIENTBOUND, version);
      TransferPacket decoded = new TransferPacket();
      decoded.decode(buf, ProtocolUtils.Direction.CLIENTBOUND, version);
      assertFalse(buf.isReadable(), "trailing bytes");
      return decoded;
    } finally {
      buf.release();
    }
  }

  @Test
  void keepsPropertiesOn264() {
    TransferPacket decoded = roundTrip(
        new TransferPacket("mc.example.com", 25565, Map.of("token", "abc", "empty", "")),
        ProtocolVersion.MINECRAFT_26_4);
    assertEquals("mc.example.com", decoded.address().getHostString());
    assertEquals(25565, decoded.address().getPort());
    assertEquals(Map.of("token", "abc", "empty", ""), decoded.properties());
  }

  @Test
  void dropsPropertiesBefore264() {
    TransferPacket decoded = roundTrip(
        new TransferPacket("mc.example.com", 25565, Map.of("token", "abc")),
        ProtocolVersion.MINECRAFT_26_3);
    assertEquals(Map.of(), decoded.properties());
  }
}
