package com.velocitypowered.proxy.protocol.netty;

import com.velocitypowered.proxy.util.except.QuietException;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.util.ByteProcessor;
import java.util.List;

public class MinecraftVarintFrameDecoder extends ByteToMessageDecoder {

  private static final QuietException BAD_LENGTH_CACHED = new QuietException("Bad packet length");
  private final VarintByteDecoder reader = new VarintByteDecoder();

  @Override
  protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {
    if (!ctx.channel().isActive()) {
      in.skipBytes(in.readableBytes());
      return;
    }

    while (in.isReadable()) {
      int varintEnd = in.forEachByte(reader);
      if (varintEnd == -1) {
        return;
      }

      if (reader.successfulDecode) {
        if (reader.readVarint < 0) {
          throw BAD_LENGTH_CACHED;
        }

        int minimumRead = reader.bytesRead + reader.readVarint;
        if (in.isReadable(minimumRead)) {
          out.add(in.retainedSlice(varintEnd + 1, reader.readVarint));
          in.skipBytes(minimumRead);
          reader.reset();
        } else {
          reader.reset();
          return;
        }
      } else {
        boolean tooBig = reader.bytesRead > 3;
        reader.reset();
        if (tooBig) {
          throw BAD_LENGTH_CACHED;
        }
      }
    }
  }

  private static class VarintByteDecoder implements ByteProcessor {
    private int readVarint;
    private int bytesRead;
    private boolean successfulDecode;

    @Override
    public boolean process(byte k) {
      readVarint |= (k & 0x7F) << bytesRead++ * 7;
      if (bytesRead > 3) {
        return false;
      }
      if ((k & 0x80) != 128) {
        successfulDecode = true;
        return false;
      }
      return true;
    }

    void reset() {
      readVarint = 0;
      bytesRead = 0;
      successfulDecode = false;
    }
  }
}
