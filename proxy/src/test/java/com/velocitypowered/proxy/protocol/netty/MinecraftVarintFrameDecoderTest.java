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

package com.velocitypowered.proxy.protocol.netty;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import com.velocitypowered.proxy.protocol.StateRegistry;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.DecoderException;
import org.junit.jupiter.api.Test;

/**
 * Tests that pre-login frames are bounded by the packet they contain before they are buffered.
 */
class MinecraftVarintFrameDecoderTest {

  private static final int MAX_FRAME_LENGTH = (1 << 21) - 1;
  private static final int LOGIN_PLUGIN_RESPONSE_ID = 0x02;
  private static final int COOKIE_RESPONSE_ID = 0x04;
  // varint id + success boolean + 1 MiB payload
  private static final int LOGIN_PLUGIN_RESPONSE_MAX = 5 + 1 + 1048576;

  private static EmbeddedChannel channel(StateRegistry state, ProtocolVersion version,
      boolean compression) {
    MinecraftVarintFrameDecoder decoder =
        new MinecraftVarintFrameDecoder(ProtocolUtils.Direction.SERVERBOUND);
    decoder.setState(state);
    decoder.setProtocolVersion(version);
    decoder.setCompressionEnabled(compression);
    return new EmbeddedChannel(decoder);
  }

  /** Writes only the frame length and packet id, as an attacker trickling a large frame would. */
  private static ByteBuf header(int frameLength, int... prefix) {
    ByteBuf buf = Unpooled.buffer();
    ProtocolUtils.writeVarInt(buf, frameLength);
    for (int value : prefix) {
      ProtocolUtils.writeVarInt(buf, value);
    }
    return buf;
  }

  private static ByteBuf frame(int packetId, int payloadLength) {
    ByteBuf buf = header(ProtocolUtils.varIntBytes(packetId) + payloadLength, packetId);
    buf.writeZero(payloadLength);
    return buf;
  }

  @Test
  void statusRejectsOversizedFrameFromHeader() {
    EmbeddedChannel channel = channel(StateRegistry.STATUS, ProtocolVersion.MINECRAFT_1_21_9, false);
    assertThrows(DecoderException.class, () -> channel.writeInbound(header(MAX_FRAME_LENGTH, 0x00)));
    channel.finishAndReleaseAll();
  }

  @Test
  void statusAcceptsRequestAndPing() {
    EmbeddedChannel channel = channel(StateRegistry.STATUS, ProtocolVersion.MINECRAFT_1_21_9, false);
    channel.writeInbound(frame(0x00, 0), frame(0x01, 8));
    ByteBuf request = channel.readInbound();
    ByteBuf ping = channel.readInbound();
    assertEquals(1, request.readableBytes());
    assertEquals(9, ping.readableBytes());
    request.release();
    ping.release();
    channel.finishAndReleaseAll();
  }

  @Test
  void statusRejectsUnknownPacket() {
    EmbeddedChannel channel = channel(StateRegistry.STATUS, ProtocolVersion.MINECRAFT_1_21_9, false);
    assertThrows(DecoderException.class, () -> channel.writeInbound(header(16, 0x05)));
    channel.finishAndReleaseAll();
  }

  @Test
  void loginPluginResponseBoundedByVanillaLimit() {
    int idBytes = ProtocolUtils.varIntBytes(LOGIN_PLUGIN_RESPONSE_ID);

    EmbeddedChannel accepted = channel(StateRegistry.LOGIN, ProtocolVersion.MINECRAFT_1_21_9, false);
    accepted.writeInbound(frame(LOGIN_PLUGIN_RESPONSE_ID, LOGIN_PLUGIN_RESPONSE_MAX));
    ByteBuf decoded = accepted.readInbound();
    assertEquals(idBytes + LOGIN_PLUGIN_RESPONSE_MAX, decoded.readableBytes());
    decoded.release();
    accepted.finishAndReleaseAll();

    EmbeddedChannel rejected = channel(StateRegistry.LOGIN, ProtocolVersion.MINECRAFT_1_21_9, false);
    assertThrows(DecoderException.class, () -> rejected.writeInbound(
        header(idBytes + LOGIN_PLUGIN_RESPONSE_MAX + 1, LOGIN_PLUGIN_RESPONSE_ID)));
    rejected.finishAndReleaseAll();
  }

  @Test
  void loginUsesConnectionProtocolVersion() {
    // The cookie response packet only exists in the login state from 1.20.5 onwards.
    EmbeddedChannel modern = channel(StateRegistry.LOGIN, ProtocolVersion.MINECRAFT_1_20_5, false);
    modern.writeInbound(frame(COOKIE_RESPONSE_ID, 2));
    ByteBuf decoded = modern.readInbound();
    decoded.release();
    modern.finishAndReleaseAll();

    EmbeddedChannel old = channel(StateRegistry.LOGIN, ProtocolVersion.MINECRAFT_1_20_3, false);
    assertThrows(DecoderException.class, () -> old.writeInbound(frame(COOKIE_RESPONSE_ID, 2)));
    old.finishAndReleaseAll();
  }

  @Test
  void loginWithCompressionValidatesUncompressedFrames() {
    EmbeddedChannel channel = channel(StateRegistry.LOGIN, ProtocolVersion.MINECRAFT_1_21_9, true);
    // data length 0 means the packet is not compressed, so its id is visible
    assertThrows(DecoderException.class, () -> channel.writeInbound(
        header(MAX_FRAME_LENGTH, 0, LOGIN_PLUGIN_RESPONSE_ID)));
    channel.finishAndReleaseAll();
  }

  @Test
  void loginWithCompressionPassesCompressedFrames() {
    EmbeddedChannel channel = channel(StateRegistry.LOGIN, ProtocolVersion.MINECRAFT_1_21_9, true);
    // a compressed frame is left to the compression decoder's limits
    channel.writeInbound(header(1024, 4096));
    assertNull(channel.readInbound());
    channel.finishAndReleaseAll();
  }

  @Test
  void playIsNotValidatedByFrameDecoder() {
    EmbeddedChannel channel = channel(StateRegistry.PLAY, ProtocolVersion.MINECRAFT_1_21_9, false);
    channel.writeInbound(header(MAX_FRAME_LENGTH, 0x00));
    assertNull(channel.readInbound());
    channel.finishAndReleaseAll();
  }
}
