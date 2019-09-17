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
    int lastReaderIndex = in.readerIndex();
    find_packets: while (in.isReadable()) {
      for (int i = 0; i < 3; i++) {
        if (!in.isReadable()) {
          break;
        }

        byte read = in.readByte();
        if (read >= 0) {
          // Make sure reader index of length buffer is returned to the beginning
          in.readerIndex(lastReaderIndex);
          int packetLength = ProtocolUtils.readVarInt(in);
          if (packetLength == 0) {
            break find_packets;
          }

          if (in.readableBytes() < packetLength) {
            break find_packets;
          }

          out.add(in.readRetainedSlice(packetLength));
          lastReaderIndex = in.readerIndex();
          continue find_packets;
        }
      }

      throw new CorruptedFrameException("VarInt too big");
    }

    in.readerIndex(lastReaderIndex);
  }
}
