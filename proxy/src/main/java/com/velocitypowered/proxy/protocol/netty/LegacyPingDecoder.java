package com.velocitypowered.proxy.protocol.netty;

import com.velocitypowered.proxy.protocol.packet.LegacyHandshake;
import com.velocitypowered.proxy.protocol.packet.LegacyPing;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import java.util.List;

public class LegacyPingDecoder extends ByteToMessageDecoder {

  @Override
  protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
    if (in.readableBytes() < 2) {
      return;
    }

    short first = in.getUnsignedByte(in.readerIndex());
    short second = in.getUnsignedByte(in.readerIndex() + 1);
    if (first == 0xfe && second == 0x01) {
      in.skipBytes(in.readableBytes());
      out.add(new LegacyPing());
    } else if (first == 0x02) {
      in.skipBytes(in.readableBytes());
      out.add(new LegacyHandshake());
    } else {
      ctx.pipeline().remove(this);
    }
  }
}
