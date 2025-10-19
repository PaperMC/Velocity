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
import com.velocitypowered.proxy.protocol.PacketCodec;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import com.velocitypowered.proxy.protocol.StateRegistry;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

/**
 * Encodes {@link MinecraftPacket} instances.
 */
public class MinecraftEncoder extends MessageToByteEncoder<MinecraftPacket> {

  private final ProtocolUtils.Direction direction;
  private StateRegistry state;
  private StateRegistry.PacketRegistry.ProtocolRegistry registry;

  /**
   * Creates a new {@code MinecraftEncoder} encoding packets for the specified {@code direction}.
   *
   * @param direction the direction to encode to
   */
  public MinecraftEncoder(ProtocolUtils.Direction direction) {
    this.direction = Preconditions.checkNotNull(direction, "direction");
    this.registry = StateRegistry.HANDSHAKE.getProtocolRegistry(
        direction, ProtocolVersion.MINIMUM_VERSION);
    this.state = StateRegistry.HANDSHAKE;
  }

  @Override
  @SuppressWarnings("unchecked")
  protected void encode(ChannelHandlerContext ctx, MinecraftPacket msg, ByteBuf out) {
    PacketCodec<MinecraftPacket> codec = (PacketCodec<MinecraftPacket>) this.registry.getCodec(msg.getClass());
    if (codec == null) {
      throw new IllegalArgumentException("No codec found for packet: " + msg.getClass());
    }

    int packetId = this.registry.getPacketId(msg);
    ProtocolUtils.writeVarInt(out, packetId);
    codec.encode(msg, out, direction, registry.version);
  }

  @Override
  @SuppressWarnings("unchecked")
  protected ByteBuf allocateBuffer(ChannelHandlerContext ctx, MinecraftPacket msg,
      boolean preferDirect) throws Exception {
    PacketCodec<MinecraftPacket> codec =
        (PacketCodec<MinecraftPacket>)
        this.registry.getCodec(msg.getClass());

    int hint = -1;
    if (codec != null) {
      hint = codec.encodeSizeHint(msg, direction, registry.version);
    }

    if (hint < 0) {
      return super.allocateBuffer(ctx, msg, preferDirect);
    }

    int packetId = this.registry.getPacketId(msg);
    int totalHint = ProtocolUtils.varIntBytes(packetId) + hint;
    return preferDirect ? ctx.alloc().ioBuffer(totalHint) : ctx.alloc().heapBuffer(totalHint);
  }

  public void setProtocolVersion(final ProtocolVersion protocolVersion) {
    this.registry = state.getProtocolRegistry(direction, protocolVersion);
  }

  public void setState(StateRegistry state) {
    this.state = state;
    this.setProtocolVersion(registry.version);
  }

  public ProtocolUtils.Direction getDirection() {
    return direction;
  }
}
