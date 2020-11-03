package com.velocitypowered.proxy.protocol.netty;

import com.velocitypowered.proxy.util.except.QuietDecoderException;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.util.ByteProcessor;
import java.util.List;

public class MinecraftVarintFrameDecoder extends ByteToMessageDecoder {

  private static final QuietDecoderException BAD_LENGTH_CACHED =
      new QuietDecoderException("Bad packet length");
  private static final QuietDecoderException VARINT_BIG_CACHED =
      new QuietDecoderException("VarInt too big");
  private final VarintByteDecoder reader = new VarintByteDecoder();

  @Override
  protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {
    if (!ctx.channel().isActive()) {
      in.clear();
      return;
    }

    reader.reset();

    int varintEnd = in.forEachByte(reader);
    if (varintEnd == -1) {
      // We tried to go beyond the end of the buffer. This is probably a good sign that the
      // buffer was too short to hold a proper varint.
      return;
    }

    if (reader.result == DecodeResult.SUCCESS) {
      if (reader.readVarint < 0) {
        throw BAD_LENGTH_CACHED;
      } else if (reader.readVarint == 0) {
        // skip over the empty packet and ignore it
        in.readerIndex(varintEnd + 1);
      } else {
        int minimumRead = reader.bytesRead + reader.readVarint;
        if (in.isReadable(minimumRead)) {
          out.add(in.retainedSlice(varintEnd + 1, reader.readVarint));
          in.skipBytes(minimumRead);
        }
      }
    } else if (reader.result == DecodeResult.TOO_BIG) {
      throw VARINT_BIG_CACHED;
    }
  }

  private static class VarintByteDecoder implements ByteProcessor {
    private int readVarint;
    private int bytesRead;
    private DecodeResult result = DecodeResult.TOO_SHORT;

    @Override
    public boolean process(byte k) {
      readVarint |= (k & 0x7F) << bytesRead++ * 7;
      if (bytesRead > 3) {
        result = DecodeResult.TOO_BIG;
        return false;
      }
      if ((k & 0x80) != 128) {
        result = DecodeResult.SUCCESS;
        return false;
      }
      return true;
    }

    void reset() {
      readVarint = 0;
      bytesRead = 0;
      result = DecodeResult.TOO_SHORT;
    }
  }

  private enum DecodeResult {
    SUCCESS,
    TOO_SHORT,
    TOO_BIG
  }
}
