package com.velocitypowered.proxy.protocol.netty;

import static com.velocitypowered.proxy.protocol.util.NettyPreconditions.checkFrame;

import com.velocitypowered.natives.compression.VelocityCompressor;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;
import java.util.List;

public class MinecraftCompressDecoder extends MessageToMessageDecoder<ByteBuf> {

  private static final int MAXIMUM_INITIAL_BUFFER_SIZE = 65536; // 64KiB

  private final int threshold;
  private final VelocityCompressor compressor;

  public MinecraftCompressDecoder(int threshold, VelocityCompressor compressor) {
    this.threshold = threshold;
    this.compressor = compressor;
  }

  @Override
  protected void decode(ChannelHandlerContext ctx, ByteBuf msg, List<Object> out) throws Exception {
    int expectedUncompressedSize = ProtocolUtils.readVarInt(msg);
    if (expectedUncompressedSize == 0) {
      // Strip the now-useless uncompressed size, this message is already uncompressed.
      out.add(msg.retainedSlice());
      msg.skipBytes(msg.readableBytes());
      return;
    }

    checkFrame(expectedUncompressedSize >= threshold,
        "Uncompressed size %s is greater than threshold %s",
        expectedUncompressedSize, threshold);
    ByteBuf uncompressed = ctx.alloc()
        .buffer(Math.min(expectedUncompressedSize, MAXIMUM_INITIAL_BUFFER_SIZE));
    try {
      compressor.inflate(msg, uncompressed);
      checkFrame(expectedUncompressedSize == uncompressed.readableBytes(),
          "Mismatched compression sizes (got %s, expected %s)",
          uncompressed.readableBytes(), expectedUncompressedSize);
      out.add(uncompressed);
    } catch (Exception e) {
      uncompressed.release();
      throw e;
    }
  }

  @Override
  public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
    compressor.dispose();
  }
}
