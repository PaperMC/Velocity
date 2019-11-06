package com.velocitypowered.proxy.protocol.netty;

import com.velocitypowered.proxy.protocol.ProtocolUtils;
import io.netty.buffer.ByteBuf;
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

    int origReaderIndex = in.readerIndex();
    for (int i = 0; i < 3; i++) {
      if (!in.isReadable()) {
        in.readerIndex(origReaderIndex);
        return;
      }

      byte read = in.readByte();
      if (read >= 0) {
        // Make sure reader index of length buffer is returned to the beginning
        in.readerIndex(origReaderIndex);
        int packetLength = ProtocolUtils.readVarInt(in);

        if (in.readableBytes() >= packetLength) {
          out.add(in.readRetainedSlice(packetLength));
        } else {
          in.readerIndex(origReaderIndex);
        }

        return;
      }
    }

    throw new CorruptedFrameException("VarInt too big");
  }
}
