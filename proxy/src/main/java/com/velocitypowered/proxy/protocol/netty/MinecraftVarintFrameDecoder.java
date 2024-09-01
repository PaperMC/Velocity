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

import com.velocitypowered.proxy.protocol.netty.TwentyOneBitVarintByteDecoder.DecodeResult;
import com.velocitypowered.proxy.util.except.QuietDecoderException;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import java.util.List;

/**
 * Frames Minecraft server packets which are prefixed by a 21-bit VarInt encoding.
 */
public class MinecraftVarintFrameDecoder extends ByteToMessageDecoder {

  private static final QuietDecoderException BAD_PACKET_LENGTH =
      new QuietDecoderException("Bad packet length");
  private static final QuietDecoderException VARINT_TOO_BIG =
      new QuietDecoderException("VarInt too big");

  @Override
  protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {
    if (!ctx.channel().isActive()) {
      in.clear();
      return;
    }

    final TwentyOneBitVarintByteDecoder reader = new TwentyOneBitVarintByteDecoder();

    int varintEnd = in.forEachByte(reader);
    if (varintEnd == -1) {
      // We tried to go beyond the end of the buffer. This is probably a good sign that the
      // buffer was too short to hold a proper varint.
      if (reader.getResult() == DecodeResult.RUN_OF_ZEROES) {
        // If the packet is literally all zeroes, we can just ignore everything.
        in.clear();
      }
      return;
    }

    switch (reader.getResult()) {
      case RUN_OF_ZEROES:
        // We didn't decode anything useful, so we can just skip over the zeroes.
        in.readerIndex(varintEnd);
        break;
      case TOO_SHORT:
        // This case shouldn't happen (we check if we only have a partial varint above), but if it
        // does, we just wait for more data.
        break;
      case TOO_BIG:
        // Invalid varint, clear the buffer and close the connection (by throwing an exception).
        in.clear();
        throw VARINT_TOO_BIG;
      case SUCCESS:
        // We decoded something. Do some sanity checks.
        int len = reader.getReadVarint();
        if (len < 0) {
          // It's a negative length, which is invalid.
          in.clear();
          throw BAD_PACKET_LENGTH;
        } else {
          int varintLength = reader.getBytesRead();
          if (in.isReadable(len + varintLength)) {
            in.readerIndex(varintEnd + 1);
            out.add(in.readRetainedSlice(len));
          }
        }
        break;
      default:
        // this should never happen
        throw new AssertionError();
    }
  }
}
