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

import com.velocitypowered.proxy.protocol.ProtocolUtils;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageEncoder;
import java.util.List;

/**
 * Handler for appending a length for Minecraft packets using composite buffers.
 */
@ChannelHandler.Sharable
public class MinecraftVarintLengthCompositeEncoder extends MessageToMessageEncoder<ByteBuf> {

  public static final MinecraftVarintLengthCompositeEncoder INSTANCE = new MinecraftVarintLengthCompositeEncoder();

  private MinecraftVarintLengthCompositeEncoder() {
  }

  @Override
  protected void encode(ChannelHandlerContext ctx, ByteBuf buf,
      List<Object> list) throws Exception {
    ByteBuf varIntBuffer = ctx.alloc().ioBuffer(ProtocolUtils.varIntBytes(buf.readableBytes()));
    ProtocolUtils.writeVarInt(varIntBuffer, buf.readableBytes());
    list.add(varIntBuffer);
    list.add(buf.retain());
  }
}
