package com.velocitypowered.proxy.protocol.netty;

import com.velocitypowered.proxy.protocol.ProtocolUtils;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.CorruptedFrameException;
import java.util.List;

public class MinecraftVarintFrameDecoder extends ByteToMessageDecoder {

  @Override
  protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
    if (!in.isReadable()) {
      return;
    }

    in.markReaderIndex();

    byte[] lenBuf = new byte[3];
    for (int i = 0; i < lenBuf.length; i++) {
      if (!in.isReadable()) {
        in.resetReaderIndex();
        return;
      }

      lenBuf[i] = in.readByte();
      if (lenBuf[i] > 0) {
        int packetLength = ProtocolUtils.readVarInt(Unpooled.wrappedBuffer(lenBuf));
        if (packetLength == 0) {
          return;
        }

        if (in.readableBytes() < packetLength) {
          in.resetReaderIndex();
          return;
        }

        out.add(in.readRetainedSlice(packetLength));
        return;
      }
    }

    throw new CorruptedFrameException("VarInt too big");
  }
}
