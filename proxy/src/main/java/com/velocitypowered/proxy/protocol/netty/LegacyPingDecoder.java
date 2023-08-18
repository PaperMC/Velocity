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

import static com.velocitypowered.proxy.protocol.util.NettyPreconditions.checkFrame;

import com.velocitypowered.proxy.protocol.packet.LegacyHandshake;
import com.velocitypowered.proxy.protocol.packet.LegacyPing;
import com.velocitypowered.proxy.protocol.packet.legacyping.LegacyMinecraftPingVersion;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Decodes Minecraft 1.3-1.6.4 server ping requests.
 */
public class LegacyPingDecoder extends ByteToMessageDecoder {

  private static final String MC_1_6_CHANNEL = "MC|PingHost";

  @Override
  protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
    if (!in.isReadable()) {
      return;
    }

    if (!ctx.channel().isActive()) {
      in.clear();
      return;
    }

    int originalReaderIndex = in.readerIndex();
    short first = in.readUnsignedByte();
    if (first == 0xfe) {
      // possibly a ping
      if (!in.isReadable()) {
        out.add(new LegacyPing(LegacyMinecraftPingVersion.MINECRAFT_1_3));
        return;
      }

      short next = in.readUnsignedByte();
      if (next == 1 && !in.isReadable()) {
        out.add(new LegacyPing(LegacyMinecraftPingVersion.MINECRAFT_1_4));
        return;
      }

      // We got a 1.6.x ping. Let's chomp off the stuff we don't need.
      out.add(readExtended16Data(in));
    } else if (first == 0x02 && in.isReadable()) {
      in.skipBytes(in.readableBytes());
      out.add(new LegacyHandshake());
    } else {
      in.readerIndex(originalReaderIndex);
      ctx.pipeline().remove(this);
    }
  }

  private static LegacyPing readExtended16Data(ByteBuf in) {
    in.skipBytes(1);
    String channelName = readLegacyString(in);
    if (!channelName.equals(MC_1_6_CHANNEL)) {
      throw new IllegalArgumentException("Didn't find correct channel");
    }
    in.skipBytes(3);
    String hostname = readLegacyString(in);
    int port = in.readInt();

    return new LegacyPing(LegacyMinecraftPingVersion.MINECRAFT_1_6, InetSocketAddress
        .createUnresolved(hostname, port));
  }

  private static String readLegacyString(ByteBuf buf) {
    int len = buf.readShort() * Character.BYTES;
    checkFrame(buf.isReadable(len), "String length %s is too large for available bytes %d",
        len, buf.readableBytes());
    String str = buf.toString(buf.readerIndex(), len, StandardCharsets.UTF_16BE);
    buf.skipBytes(len);
    return str;
  }
}
