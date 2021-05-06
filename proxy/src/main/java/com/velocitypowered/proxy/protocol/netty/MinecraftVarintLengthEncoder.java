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

import com.velocitypowered.natives.encryption.JavaVelocityCipher;
import com.velocitypowered.natives.util.Natives;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import io.netty.util.concurrent.FastThreadLocal;

@ChannelHandler.Sharable
public class MinecraftVarintLengthEncoder extends MessageToByteEncoder<ByteBuf> {

  public static final MinecraftVarintLengthEncoder INSTANCE = new MinecraftVarintLengthEncoder();
  public static final boolean IS_JAVA_CIPHER = Natives.cipher.get() == JavaVelocityCipher.FACTORY;
  private static final ThreadLocal<Boolean> SKIP = ThreadLocal.withInitial(() -> false);

  private MinecraftVarintLengthEncoder() {
  }

  @Override
  protected void encode(ChannelHandlerContext ctx, ByteBuf msg, ByteBuf out) throws Exception {
    if (SKIP.get()) {
      SKIP.set(false);
    } else {
      ProtocolUtils.writeVarInt(out, msg.readableBytes());
      out.writeBytes(msg);
    }
  }

  @Override
  protected ByteBuf allocateBuffer(ChannelHandlerContext ctx, ByteBuf msg, boolean preferDirect)
      throws Exception {
    if (SKIP.get()) {
      return msg.retain();
    } else {
      int anticipatedRequiredCapacity = ProtocolUtils.varIntBytes(msg.readableBytes())
          + msg.readableBytes();
      return IS_JAVA_CIPHER
          ? ctx.alloc().heapBuffer(anticipatedRequiredCapacity)
          : ctx.alloc().directBuffer(anticipatedRequiredCapacity);
    }
  }

  public void skipNextPacket() {
    SKIP.set(true);
  }
}
