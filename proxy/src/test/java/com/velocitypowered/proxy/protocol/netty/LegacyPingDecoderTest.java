/*
 * Copyright (C) 2018-2026 Velocity Contributors
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

package com.velocitypowered.proxy.protocol.netty;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.velocitypowered.proxy.protocol.packet.LegacyPingPacket;
import com.velocitypowered.proxy.protocol.packet.legacyping.LegacyMinecraftPingVersion;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

class LegacyPingDecoderTest {

  private static final byte[] MODERN_PACKET = {(byte) 0xFE, 0x01, 0x00, 0x01, 0x02};

  @Test
  void forwardsUnfragmentedModernPacket() {
    EmbeddedChannel channel = new EmbeddedChannel(new LegacyPingDecoder());
    try {
      assertTrue(channel.writeInbound(Unpooled.wrappedBuffer(MODERN_PACKET)));
      assertModernPacketReceived(channel);
    } finally {
      channel.finishAndReleaseAll();
    }
  }

  @Test
  void forwardsModernPacketSplitAfterFirstByte() {
    assertModernPacketForwarded(1);
  }

  @Test
  void forwardsModernPacketSplitAfterSecondByte() {
    assertModernPacketForwarded(2);
  }

  @Test
  void emitsSingleByteLegacyPingAfterGracePeriod() {
    assertLegacyPing(new byte[]{(byte) 0xFE}, LegacyMinecraftPingVersion.MINECRAFT_1_3);
  }

  @Test
  void emitsTwoByteLegacyPingAfterGracePeriod() {
    assertLegacyPing(new byte[]{(byte) 0xFE, 0x01},
        LegacyMinecraftPingVersion.MINECRAFT_1_4);
  }

  private static void assertModernPacketForwarded(int splitIndex) {
    EmbeddedChannel channel = new EmbeddedChannel(new LegacyPingDecoder());
    try {
      assertFalse(channel.writeInbound(Unpooled.wrappedBuffer(MODERN_PACKET, 0, splitIndex)));
      assertTrue(channel.writeInbound(
          Unpooled.wrappedBuffer(MODERN_PACKET, splitIndex, MODERN_PACKET.length - splitIndex)));
      assertModernPacketReceived(channel);
    } finally {
      channel.finishAndReleaseAll();
    }
  }

  private static void assertModernPacketReceived(EmbeddedChannel channel) {
    ByteBuf forwarded = channel.readInbound();
    try {
      assertEquals(ByteBufUtil.hexDump(MODERN_PACKET), ByteBufUtil.hexDump(forwarded));
    } finally {
      forwarded.release();
    }
    assertNull(channel.readInbound());
  }

  private static void assertLegacyPing(byte[] request, LegacyMinecraftPingVersion version) {
    EmbeddedChannel channel = new EmbeddedChannel(new LegacyPingDecoder());
    try {
      assertFalse(channel.writeInbound(Unpooled.wrappedBuffer(request)));
      assertNull(channel.readInbound());

      channel.advanceTimeBy(1, TimeUnit.SECONDS);
      channel.runScheduledPendingTasks();

      LegacyPingPacket ping = assertInstanceOf(LegacyPingPacket.class, channel.readInbound());
      assertEquals(version, ping.getVersion());
      assertNull(channel.readInbound());
    } finally {
      channel.finishAndReleaseAll();
    }
  }
}
