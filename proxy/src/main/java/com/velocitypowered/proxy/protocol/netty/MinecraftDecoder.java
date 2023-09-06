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

package com.velocitypowered.proxy.protocol.netty;

import com.google.common.base.Preconditions;
import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.proxy.protocol.MinecraftPacket;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import com.velocitypowered.proxy.protocol.StateRegistry;
import com.velocitypowered.proxy.protocol.netty.data.CompressedPacket;
import com.velocitypowered.proxy.protocol.netty.data.IdentifiedPacket;
import com.velocitypowered.proxy.protocol.netty.data.UncompressedPacket;
import com.velocitypowered.proxy.util.except.QuietRuntimeException;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.CorruptedFrameException;
import io.netty.handler.codec.MessageToMessageDecoder;
import java.util.List;

/**
 * Decodes Minecraft packets.
 */
public class MinecraftDecoder extends MessageToMessageDecoder<IdentifiedPacket> {

  public static final boolean DEBUG = Boolean.getBoolean("velocity.packet-decode-logging");
  private static final QuietRuntimeException DECODE_FAILED =
      new QuietRuntimeException("A packet did not decode successfully (invalid data). If you are a "
          + "developer, launch Velocity with -Dvelocity.packet-decode-logging=true to see more.");

  private final ProtocolUtils.Direction direction;
  private StateRegistry state;
  private StateRegistry.PacketRegistry.ProtocolRegistry registry;

  /**
   * Creates a new {@code MinecraftDecoder} decoding packets from the specified {@code direction}.
   *
   * @param direction the direction from which we decode from
   */
  public MinecraftDecoder(ProtocolUtils.Direction direction) {
    this.direction = Preconditions.checkNotNull(direction, "direction");
    this.registry = StateRegistry.HANDSHAKE.getProtocolRegistry(
        direction, ProtocolVersion.MINIMUM_VERSION);
    this.state = StateRegistry.HANDSHAKE;
  }

  @Override
  protected void decode(ChannelHandlerContext ctx, IdentifiedPacket msg, List<Object> out)
      throws Exception {
    int packetId = msg.getPacketId();
    MinecraftPacket packet = registry.createPacket(packetId);
    if (packet == null) {
      ctx.fireChannelRead(msg);
    } else {
      ByteBuf uncompressedBuf;
      if (msg instanceof UncompressedPacket) {
        uncompressedBuf = ((UncompressedPacket) msg).getPacketBuf();
      } else if (msg instanceof CompressedPacket) {
        uncompressedBuf = ((CompressedPacket) msg).decompress(ctx.alloc());
      } else {
        throw new IllegalArgumentException("Unsupported identified packet type.");
      }

      if (!ctx.channel().isActive() || !uncompressedBuf.isReadable()) {
        uncompressedBuf.release();
        return;
      }

      try {
        ProtocolUtils.readVarInt(uncompressedBuf);
        doLengthSanityChecks(uncompressedBuf, packet);

        try {
          packet.decode(uncompressedBuf, direction, registry.version);
        } catch (Exception e) {
          e.printStackTrace();
          throw handleDecodeFailure(e, packet, packetId);
        }

        if (uncompressedBuf.isReadable()) {
          throw handleOverflow(packet,
              uncompressedBuf.readerIndex(), uncompressedBuf.writerIndex());
        }
        ctx.fireChannelRead(packet);
      } finally {
        uncompressedBuf.release();
      }
    }
  }

  private void doLengthSanityChecks(ByteBuf buf, MinecraftPacket packet) throws Exception {
    int expectedMinLen = packet.expectedMinLength(buf, direction, registry.version);
    int expectedMaxLen = packet.expectedMaxLength(buf, direction, registry.version);
    if (expectedMaxLen != -1 && buf.readableBytes() > expectedMaxLen) {
      throw handleOverflow(packet, expectedMaxLen, buf.readableBytes());
    }
    if (buf.readableBytes() < expectedMinLen) {
      throw handleUnderflow(packet, expectedMaxLen, buf.readableBytes());
    }
  }

  private Exception handleOverflow(MinecraftPacket packet, int expected, int actual) {
    if (DEBUG) {
      return new CorruptedFrameException("Packet sent for " + packet.getClass() + " was too "
          + "big (expected " + expected + " bytes, got " + actual + " bytes)");
    } else {
      return DECODE_FAILED;
    }
  }

  private Exception handleUnderflow(MinecraftPacket packet, int expected, int actual) {
    if (DEBUG) {
      return new CorruptedFrameException("Packet sent for " + packet.getClass() + " was too "
          + "small (expected " + expected + " bytes, got " + actual + " bytes)");
    } else {
      return DECODE_FAILED;
    }
  }

  private Exception handleDecodeFailure(Exception cause, MinecraftPacket packet, int packetId) {
    if (DEBUG) {
      return new CorruptedFrameException(
          "Error decoding " + packet.getClass() + " " + getExtraConnectionDetail(packetId), cause);
    } else {
      return DECODE_FAILED;
    }
  }

  private String getExtraConnectionDetail(int packetId) {
    return "Direction " + direction + " Protocol " + registry.version + " State " + state
        + " ID " + Integer.toHexString(packetId);
  }

  public void setProtocolVersion(ProtocolVersion protocolVersion) {
    this.registry = state.getProtocolRegistry(direction, protocolVersion);
  }

  public void setState(StateRegistry state) {
    this.state = state;
    this.setProtocolVersion(registry.version);
  }
}
