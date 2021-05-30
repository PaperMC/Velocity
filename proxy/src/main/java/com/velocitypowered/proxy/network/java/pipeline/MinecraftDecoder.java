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

package com.velocitypowered.proxy.network.java.pipeline;

import com.google.common.base.Preconditions;
import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.proxy.network.ProtocolUtils;
import com.velocitypowered.proxy.network.java.states.ProtocolStates;
import com.velocitypowered.proxy.network.packet.Packet;
import com.velocitypowered.proxy.network.packet.PacketDirection;
import com.velocitypowered.proxy.network.packet.PacketReader;
import com.velocitypowered.proxy.network.registry.packet.PacketRegistryMap;
import com.velocitypowered.proxy.network.registry.protocol.ProtocolRegistry;
import com.velocitypowered.proxy.util.except.QuietRuntimeException;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.CorruptedFrameException;
import org.checkerframework.checker.nullness.qual.Nullable;

public class MinecraftDecoder extends ChannelInboundHandlerAdapter {

  public static final boolean DEBUG = Boolean.getBoolean("velocity.packet-decode-logging");
  private static final QuietRuntimeException DECODE_FAILED =
      new QuietRuntimeException("A packet did not decode successfully (invalid data). If you are a "
          + "developer, launch Velocity with -Dvelocity.packet-decode-logging=true to see more.");

  private final PacketDirection direction;
  private ProtocolVersion version;
  private ProtocolRegistry state;
  private PacketRegistryMap registry;

  /**
   * Creates a new {@code MinecraftDecoder} decoding packets from the specified {@code direction}.
   *
   * @param direction the direction from which we decode from
   */
  public MinecraftDecoder(PacketDirection direction) {
    this.direction = Preconditions.checkNotNull(direction, "direction");
    this.state = ProtocolStates.HANDSHAKE;
    this.version = ProtocolVersion.MINIMUM_VERSION;
    this.registry = this.state.lookup(direction, ProtocolVersion.MINIMUM_VERSION);
  }

  @Override
  public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
    if (msg instanceof ByteBuf) {
      ByteBuf buf = (ByteBuf) msg;
      tryDecode(ctx, buf);
    } else {
      ctx.fireChannelRead(msg);
    }
  }

  private void tryDecode(ChannelHandlerContext ctx, ByteBuf buf) throws Exception {
    if (!ctx.channel().isActive() || !buf.isReadable()) {
      buf.release();
      return;
    }

    int originalReaderIndex = buf.readerIndex();
    int packetId = ProtocolUtils.readVarInt(buf);
    Packet packet;
    try {
      packet = this.readPacket(packetId, buf);
    } catch (Exception e) {
      throw handleDecodeFailure(e, packetId);
    }

    if (packet == null) {
      buf.readerIndex(originalReaderIndex);
      ctx.fireChannelRead(buf);
    } else {
      try {
        if (buf.isReadable()) {
          throw handleOverflow(buf.readerIndex(), buf.writerIndex());
        }
        ctx.fireChannelRead(packet);
      } finally {
        buf.release();
      }
    }
  }

  private @Nullable Packet readPacket(int packetId, ByteBuf buf) throws Exception {
    PacketReader<? extends Packet> reader = this.registry.lookupReader(packetId, this.version);
    if (reader == null) {
      return null;
    }

    int expectedMinLen = reader.expectedMinLength(buf, version);
    int expectedMaxLen = reader.expectedMaxLength(buf, version);
    if (expectedMaxLen != -1 && buf.readableBytes() > expectedMaxLen) {
      throw handleOverflow(expectedMaxLen, buf.readableBytes());
    }
    if (buf.readableBytes() < expectedMinLen) {
      throw handleUnderflow(expectedMaxLen, buf.readableBytes());
    }

    return reader.read(buf, version);
  }

  private Exception handleOverflow(int expected, int actual) {
    if (DEBUG) {
      return new CorruptedFrameException("Packet sent was too big (expected "
          + expected + " bytes, got " + actual + " bytes)");
    } else {
      return DECODE_FAILED;
    }
  }

  private Exception handleUnderflow(int expected, int actual) {
    if (DEBUG) {
      return new CorruptedFrameException("Packet was too small (expected " + expected
          + " bytes, got " + actual + " bytes)");
    } else {
      return DECODE_FAILED;
    }
  }

  private Exception handleDecodeFailure(Exception cause, int packetId) {
    if (DEBUG) {
      Class<? extends Packet> packetClass = this.registry.lookupPacket(packetId);
      return new CorruptedFrameException(
          "Error decoding " + packetClass + " " + getExtraConnectionDetail(packetId), cause);
    } else {
      return DECODE_FAILED;
    }
  }

  private String getExtraConnectionDetail(int packetId) {
    return "Direction " + direction + " Protocol " + version + " State " + state
        + " ID " + Integer.toHexString(packetId);
  }

  public void setProtocolVersion(ProtocolVersion protocolVersion) {
    this.version = protocolVersion;
    this.registry = state.lookup(direction, protocolVersion);
  }

  public void setState(ProtocolRegistry state) {
    this.state = state;
    this.setProtocolVersion(this.version);
  }
}
