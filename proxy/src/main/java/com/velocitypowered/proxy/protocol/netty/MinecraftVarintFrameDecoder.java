package com.velocitypowered.proxy.protocol.netty;

import com.velocitypowered.proxy.protocol.ProtocolUtils;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import java.util.List;

public class MinecraftVarintFrameDecoder extends ByteToMessageDecoder {

  @Override
  protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
    while (in.isReadable()) {
      int ri = in.readerIndex();
      int packetLength = ProtocolUtils.readVarInt(in);

      if (in.readableBytes() >= packetLength) {
        out.add(in.readBytes(packetLength));
      } else {
        in.readerIndex(ri);
        break;
      }
    }
  }
}
