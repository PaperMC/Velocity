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

import com.velocitypowered.proxy.protocol.netty.VarintByteDecoder.DecodeResult;
import com.velocitypowered.proxy.util.except.QuietDecoderException;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import java.util.List;

public class MinecraftVarintFrameDecoder extends ByteToMessageDecoder {

  private static final QuietDecoderException BAD_LENGTH_CACHED =
      new QuietDecoderException("Bad packet length");
  private static final QuietDecoderException VARINT_BIG_CACHED =
      new QuietDecoderException("VarInt too big");

  @Override
  protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {
    if (!ctx.channel().isActive()) {
      in.clear();
      return;
    }

    final VarintByteDecoder reader = new VarintByteDecoder();

    int varintEnd = in.forEachByte(reader);
    if (varintEnd == -1) {
      // We tried to go beyond the end of the buffer. This is probably a good sign that the
      // buffer was too short to hold a proper varint.
      return;
    }

    if (reader.getResult() == DecodeResult.SUCCESS) {
      int readVarint = reader.getReadVarint();
      int bytesRead = reader.getBytesRead();
      if (readVarint < 0) {
        in.clear();
        throw BAD_LENGTH_CACHED;
      } else if (readVarint == 0) {
        // skip over the empty packet and ignore it
        in.readerIndex(varintEnd + 1);
      } else {
        int minimumRead = bytesRead + readVarint;
        if (in.isReadable(minimumRead)) {
          out.add(in.retainedSlice(varintEnd + 1, readVarint));
          in.skipBytes(minimumRead);
        }
      }
    } else if (reader.getResult() == DecodeResult.TOO_BIG) {
      in.clear();
      throw VARINT_BIG_CACHED;
    }
  }
}
