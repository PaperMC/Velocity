package com.velocitypowered.proxy.protocol.netty;

import com.google.common.base.Preconditions;
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
    int uncompressedSize = ProtocolUtils.readVarInt(msg);
    if (uncompressedSize == 0) {
      // Strip the now-useless uncompressed size, this message is already uncompressed.
      out.add(msg.retainedSlice());
      msg.skipBytes(msg.readableBytes());
      return;
    }

    Preconditions.checkState(uncompressedSize >= threshold,
        "Uncompressed size %s is greater than threshold %s",
        uncompressedSize, threshold);
    ByteBuf uncompressed = ctx.alloc()
        .buffer(Math.min(uncompressedSize, MAXIMUM_INITIAL_BUFFER_SIZE));
    try {
      compressor.inflate(msg, uncompressed);
      Preconditions.checkState(uncompressedSize == uncompressed.readableBytes(),
          "Mismatched compression sizes");
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
