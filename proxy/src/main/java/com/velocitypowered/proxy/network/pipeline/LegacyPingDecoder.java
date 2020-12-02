package com.velocitypowered.proxy.network.pipeline;

import static com.velocitypowered.proxy.network.NettyPreconditions.checkFrame;

import com.velocitypowered.proxy.network.packet.legacy.LegacyHandshakePacket;
import com.velocitypowered.proxy.network.packet.legacy.LegacyMinecraftPingVersion;
import com.velocitypowered.proxy.network.packet.legacy.LegacyPingPacket;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;

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
        out.add(new LegacyPingPacket(LegacyMinecraftPingVersion.MINECRAFT_1_3));
        return;
      }

      short next = in.readUnsignedByte();
      if (next == 1 && !in.isReadable()) {
        out.add(new LegacyPingPacket(LegacyMinecraftPingVersion.MINECRAFT_1_4));
        return;
      }

      // We got a 1.6.x ping. Let's chomp off the stuff we don't need.
      out.add(readExtended16Data(in));
    } else if (first == 0x02 && in.isReadable()) {
      in.skipBytes(in.readableBytes());
      out.add(new LegacyHandshakePacket());
    } else {
      in.readerIndex(originalReaderIndex);
      ctx.pipeline().remove(this);
    }
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
