package com.velocitypowered.proxy.protocol.netty;

import com.velocitypowered.proxy.protocol.ProtocolUtils;
import com.velocitypowered.proxy.util.except.QuietException;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import java.util.List;

public class MinecraftVarintFrameDecoder extends ByteToMessageDecoder {

  private static final QuietException BAD_LENGTH_CACHED = new QuietException("Bad packet length");

  @Override
  protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {
    if (!ctx.channel().isActive()) {
      in.skipBytes(in.readableBytes());
      return;
    }

    read_lens: while (in.isReadable()) {
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
            out.add(in.readBytes(packetLength));
            continue read_lens;
          } else {
            in.readerIndex(origReaderIndex);
            return;
          }
        }
      }

      throw BAD_LENGTH_CACHED;
    }
  }
}
