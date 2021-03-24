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

package com.velocitypowered.proxy.protocol.netty;

import com.google.common.base.Preconditions;
import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.proxy.protocol.MinecraftPacket;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import com.velocitypowered.proxy.protocol.StateRegistry;
import com.velocitypowered.proxy.util.except.QuietDecoderException;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.CorruptedFrameException;

public class MinecraftDecoder extends ChannelInboundHandlerAdapter {

  public static final boolean DEBUG = Boolean.getBoolean("velocity.packet-decode-logging");
  private static final QuietDecoderException DECODE_FAILED =
      new QuietDecoderException("A packet did not decode successfully (invalid data). If you are a "
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
    this.registry = direction.getProtocolRegistry(StateRegistry.HANDSHAKE,
        ProtocolVersion.MINIMUM_VERSION);
    this.state = StateRegistry.HANDSHAKE;
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
    if (!ctx.channel().isActive()) {
      buf.release();
      return;
    }

    int originalReaderIndex = buf.readerIndex();
    int packetId = ProtocolUtils.readVarInt(buf);
    MinecraftPacket packet = this.registry.createPacket(packetId);
    if (packet == null) {
      buf.readerIndex(originalReaderIndex);
      ctx.fireChannelRead(buf);
    } else {
      try {
        try {
          packet.decode(buf, direction, registry.version);
        } catch (Exception e) {
          throw handleDecodeFailure(e, packet, packetId);
        }

        if (buf.isReadable()) {
          throw handleNotReadEnough(packet, packetId);
        }
        ctx.fireChannelRead(packet);
      } finally {
        buf.release();
      }
    }
  }

  private Exception handleNotReadEnough(MinecraftPacket packet, int packetId) {
    if (DEBUG) {
      return new CorruptedFrameException("Did not read full packet for " + packet.getClass() + " "
          + getExtraConnectionDetail(packetId));
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
    this.registry = direction.getProtocolRegistry(state, protocolVersion);
  }

  public void setState(StateRegistry state) {
    this.state = state;
    this.setProtocolVersion(registry.version);
  }
}
