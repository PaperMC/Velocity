package com.velocitypowered.proxy.protocol.netty;

import static com.velocitypowered.natives.util.MoreByteBufUtils.ensureCompatible;
import static com.velocitypowered.natives.util.MoreByteBufUtils.preferredBuffer;
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
  protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
    int expectedSize = ProtocolUtils.readVarInt(in);
    if (expectedSize == 0) {
      // Strip the now-useless uncompressed size, this message is already uncompressed.
      out.add(in.retainedSlice());
      in.skipBytes(in.readableBytes());
      return;
    }

    checkFrame(expectedSize >= threshold, "Uncompressed size %s is greater than threshold %s",
        expectedSize, threshold);
    ByteBuf compatibleIn = ensureCompatible(ctx.alloc(), compressor, in);
    int initialCapacity = Math.min(expectedSize, MAXIMUM_INITIAL_BUFFER_SIZE);
    ByteBuf uncompressed = preferredBuffer(ctx.alloc(), compressor, initialCapacity);
    try {
      compressor.inflate(compatibleIn, uncompressed);
      checkFrame(expectedSize == uncompressed.readableBytes(),
          "Mismatched compression sizes (got %s, expected %s)",
          uncompressed.readableBytes(), expectedSize);
      out.add(uncompressed);
    } catch (Exception e) {
      uncompressed.release();
      throw e;
    } finally {
      compatibleIn.release();
    }
  }

  @Override
  public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
    compressor.dispose();
  }
}
