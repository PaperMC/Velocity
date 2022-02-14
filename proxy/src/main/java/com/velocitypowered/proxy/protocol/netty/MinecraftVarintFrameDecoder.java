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

import com.velocitypowered.proxy.Velocity;
import com.velocitypowered.proxy.VelocityServer;
import com.velocitypowered.proxy.config.VelocityConfiguration;
import com.velocitypowered.proxy.firewall.FirewallManager;
import com.velocitypowered.proxy.protocol.netty.VarintByteDecoder.DecodeResult;
import com.velocitypowered.proxy.util.except.QuietDecoderException;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.List;

public class MinecraftVarintFrameDecoder extends ByteToMessageDecoder {

  private static final QuietDecoderException BAD_LENGTH_CACHED =
      new QuietDecoderException("Bad packet length");
  private static final QuietDecoderException VARINT_BIG_CACHED =
      new QuietDecoderException("VarInt too big");

  private final VelocityServer server;

  public MinecraftVarintFrameDecoder(final VelocityServer server) {
    this.server = server;
  }

  @Override
  protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {
    if (!ctx.channel().isActive()) {
      in.clear();
      return;
    }

    InetAddress address = ((InetSocketAddress)ctx.channel().remoteAddress()).getAddress();
    String ip = address.getHostAddress();

    if (server.getConfiguration().getFirewall().isNettyChecks() &&
            !FirewallManager.whitelistedAddresses.contains(ip) &&
            in.readableBytes() > server.getConfiguration().getFirewall().getMaxInvalidPacketSize()) {
      ctx.channel().flush();
      ctx.channel().close();
      ctx.close();
      in.resetReaderIndex();
      in.discardReadBytes();
      return;
    }

    final VarintByteDecoder reader = new VarintByteDecoder();

    int varintEnd = in.forEachByte(reader);
    if (varintEnd == -1) {
      // We tried to go beyond the end of the buffer. This is probably a good sign that the
      // buffer was too short to hold a proper varint.
      if (reader.getResult() == DecodeResult.RUN_OF_ZEROES) {
        // Special case where the entire packet is just a run of zeroes. We ignore them all.
        in.clear();
      }
      return;
    }

    switch (reader.getResult()) {
      case RUN_OF_ZEROES:
        // this will return to the point where the next varint starts
        in.readerIndex(varintEnd);
        break;
      case SUCCESS:
        int readVarint = reader.getReadVarint();
        int bytesRead = reader.getBytesRead();
        if (readVarint < 0) {
          in.clear();
          ctx.channel().close();
          throw BAD_LENGTH_CACHED;
        } else if (readVarint == 0) {
          // skip over the empty packet(s) and ignore it
          in.readerIndex(varintEnd + 1);
        } else {
          int minimumRead = bytesRead + readVarint;
          if (in.isReadable(minimumRead)) {
            out.add(in.retainedSlice(varintEnd + 1, readVarint));
            in.skipBytes(minimumRead);
          }
        }
        break;
      case TOO_BIG:
        in.clear();
        ctx.channel().close();
        throw VARINT_BIG_CACHED;
    }
  }
}
