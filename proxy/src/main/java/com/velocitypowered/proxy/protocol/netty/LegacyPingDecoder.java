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

import com.velocitypowered.proxy.protocol.packet.LegacyHandshakePacket;
import com.velocitypowered.proxy.protocol.packet.LegacyPingPacket;
import com.velocitypowered.proxy.protocol.packet.legacyping.LegacyMinecraftPingVersion;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Decodes Minecraft 1.3-1.6.4 server ping requests.
 */
public class LegacyPingDecoder extends ByteToMessageDecoder {

  private static final String MC_1_6_CHANNEL = "MC|PingHost";
  // FE and FE 01 are complete legacy pings, but also the beginning of a modern 254-byte frame.
  // Give a fragmented modern frame a chance to supply its packet ID before choosing legacy.
  private static final long LEGACY_PING_GRACE_PERIOD_MILLIS = 100;

  private ScheduledFuture<?> pendingLegacyPing;
  private int pendingLegacyPingLength;

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
        in.readerIndex(originalReaderIndex);
        scheduleLegacyPing(ctx, 1, LegacyMinecraftPingVersion.MINECRAFT_1_3);
        return;
      }

      short next = in.readUnsignedByte();
      if (next == 1) {
        if (!in.isReadable()) {
          in.readerIndex(originalReaderIndex);
          scheduleLegacyPing(ctx, 2, LegacyMinecraftPingVersion.MINECRAFT_1_4);
          return;
        }
        if (in.getUnsignedByte(in.readerIndex()) == 0xFA) {
          cancelPendingLegacyPing();
          out.add(readExtended16Data(in));
          return;
        }
      }

      // Not a legacy ping. Reset and let the modern decoder handle it.
      cancelPendingLegacyPing();
      in.readerIndex(originalReaderIndex);
      ctx.pipeline().remove(this);
    } else if (first == 0x02 && in.isReadable()) {
      cancelPendingLegacyPing();
      in.skipBytes(in.readableBytes());
      out.add(new LegacyHandshakePacket());
    } else {
      cancelPendingLegacyPing();
      in.readerIndex(originalReaderIndex);
      ctx.pipeline().remove(this);
    }
  }

  private void scheduleLegacyPing(ChannelHandlerContext ctx, int length,
                                  LegacyMinecraftPingVersion version) {
    if (pendingLegacyPing != null && pendingLegacyPingLength == length) {
      return;
    }

    cancelPendingLegacyPing();
    pendingLegacyPingLength = length;
    pendingLegacyPing = ctx.executor().schedule(() -> {
      pendingLegacyPing = null;
      if (!ctx.channel().isActive() || pendingLegacyPingLength != length) {
        return;
      }

      ByteBuf buffer = internalBuffer();
      if (buffer.readableBytes() != length) {
        pendingLegacyPingLength = 0;
        return;
      }

      buffer.skipBytes(length);
      pendingLegacyPingLength = 0;
      ctx.fireChannelRead(new LegacyPingPacket(version));
      ctx.fireChannelReadComplete();
      if (ctx.pipeline().context(this) != null) {
        ctx.pipeline().remove(this);
      }
    }, LEGACY_PING_GRACE_PERIOD_MILLIS, TimeUnit.MILLISECONDS);
  }

  private void cancelPendingLegacyPing() {
    if (pendingLegacyPing != null) {
      pendingLegacyPing.cancel(false);
      pendingLegacyPing = null;
    }
    pendingLegacyPingLength = 0;
  }

  @Override
  protected void handlerRemoved0(ChannelHandlerContext ctx) throws Exception {
    cancelPendingLegacyPing();
    super.handlerRemoved0(ctx);
  }

  @Override
  public void channelInactive(ChannelHandlerContext ctx) throws Exception {
    cancelPendingLegacyPing();
    super.channelInactive(ctx);
  }

  private static LegacyPingPacket readExtended16Data(ByteBuf in) {
    in.skipBytes(1);
    String channelName = readLegacyString(in);
    if (!channelName.equals(MC_1_6_CHANNEL)) {
      throw new IllegalArgumentException("Didn't find correct channel");
    }
    in.skipBytes(3);
    String hostname = readLegacyString(in);
    int port = in.readInt();

    return new LegacyPingPacket(LegacyMinecraftPingVersion.MINECRAFT_1_6, InetSocketAddress
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
