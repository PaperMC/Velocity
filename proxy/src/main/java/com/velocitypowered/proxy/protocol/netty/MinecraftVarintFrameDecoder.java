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

  private final byte[] lenBuf = new byte[3];

  @Override
  protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
    if (!in.isReadable()) {
      return;
    }

    Arrays.fill(lenBuf, (byte) 0);
    int origReaderIndex = in.readerIndex();

    ByteBuf wrappedBuf = Unpooled.wrappedBuffer(lenBuf);
    for (int i = 0; i < lenBuf.length; i++) {
      if (!in.isReadable()) {
        in.readerIndex(origReaderIndex);
        return;
      }

      lenBuf[i] = in.readByte();
      if (lenBuf[i] > 0) {
        int packetLength = ProtocolUtils.readVarInt(wrappedBuf);
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
  }
}
