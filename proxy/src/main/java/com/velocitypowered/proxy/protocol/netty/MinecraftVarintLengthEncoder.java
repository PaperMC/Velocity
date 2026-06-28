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

import com.velocitypowered.natives.encryption.JavaVelocityCipher;
import com.velocitypowered.natives.util.Natives;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

/**
 * Handler for appending a length for Minecraft packets.
 */
@ChannelHandler.Sharable
public class MinecraftVarintLengthEncoder extends MessageToByteEncoder<ByteBuf> {

  public static final MinecraftVarintLengthEncoder INSTANCE = new MinecraftVarintLengthEncoder();

  static final boolean IS_JAVA_CIPHER = Natives.cipher.get() == JavaVelocityCipher.FACTORY;

  private MinecraftVarintLengthEncoder() {
  }

  @Override
  protected void encode(ChannelHandlerContext ctx, ByteBuf buf, ByteBuf out) throws Exception {
    // bVelocity: collapse the length varint and payload into a single outbound buffer instead of
    // allocating a separate 1-5 byte length buffer per packet and handing two ByteBufs downstream.
    // The previous MessageToMessageEncoder implementation allocated a tiny length buffer on every
    // outbound packet (tens of thousands of times per second on a loaded proxy), each flowing
    // through the allocator and cipher's ensureCompatible path independently. Writing the length
    // inline reuses the single pre-allocated `out` buffer (sized via allocateBuffer below), which
    // downstream cipher/compressor handlers process as one buffer.
    ProtocolUtils.writeVarInt(out, buf.readableBytes());
    out.writeBytes(buf);
  }

  @Override
  protected ByteBuf allocateBuffer(ChannelHandlerContext ctx, ByteBuf buf, boolean preferDirect)
      throws Exception {
    final int length = buf.readableBytes();
    final int capacity = ProtocolUtils.varIntBytes(length) + length;
    return IS_JAVA_CIPHER
        ? ctx.alloc().heapBuffer(capacity)
        : ctx.alloc().directBuffer(capacity);
  }
}
