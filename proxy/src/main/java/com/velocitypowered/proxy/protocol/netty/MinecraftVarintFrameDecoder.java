package com.velocitypowered.proxy.protocol.netty;

import com.velocitypowered.proxy.protocol.ProtocolUtils;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.CorruptedFrameException;
import java.util.Arrays;
import java.util.List;

public class MinecraftVarintFrameDecoder extends ByteToMessageDecoder {

  @Override
  protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
    if (!in.isReadable()) {
      return;
    }

    ByteBuf lenBuf = ctx.alloc().buffer(3).writeZero(3);
    int origReaderIndex = in.readerIndex();
    try {
      for (int i = 0; i < 3; i++) {
        if (!in.isReadable()) {
          in.readerIndex(origReaderIndex);
          return;
        }

        byte read = in.readByte();
        lenBuf.setByte(i, read);
        if (read > 0) {
          // Make sure reader index of length buffer is returned to the beginning
          lenBuf.readerIndex(0);
          int packetLength = ProtocolUtils.readVarInt(lenBuf);
          if (packetLength == 0) {
            return;
          }

          if (in.readableBytes() < packetLength) {
            in.readerIndex(origReaderIndex);
            return;
          }

          out.add(in.readRetainedSlice(packetLength));
          return;
        }
      }

      throw new CorruptedFrameException("VarInt too big");
    } finally {
      lenBuf.release();
    }
  }
}
