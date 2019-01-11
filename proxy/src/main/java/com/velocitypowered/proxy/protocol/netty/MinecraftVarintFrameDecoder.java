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

    ByteBuf lenBuf = ctx.alloc().buffer(3);
    int origReaderIndex = in.readerIndex();
    try {
      for (int i = 0; i < 3; i++) {
        if (!in.isReadable()) {
          in.readerIndex(origReaderIndex);
          return;
        }

        byte read = in.readByte();
        lenBuf.writeByte(read);
        if (read > 0) {
          int packetLength = ProtocolUtils.readVarInt(lenBuf.slice());
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
