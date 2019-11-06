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

  private static final int SOFT_MAXIMUM_UNCOMPRESSED_SIZE = 2 * 1024 * 1024; // 2MiB
  private static final int HARD_MAXIMUM_UNCOMPRESSED_SIZE = 16 * 1024 * 1024; // 16MiB

  private final int threshold;
  private final VelocityCompressor compressor;

  public MinecraftCompressDecoder(int threshold, VelocityCompressor compressor) {
    this.threshold = threshold;
    this.compressor = compressor;
  }

  @Override
  protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
    int claimedUncompressedSize = ProtocolUtils.readVarInt(in);
    if (claimedUncompressedSize == 0) {
      // Strip the now-useless uncompressed size, this message is already uncompressed.
      out.add(in.retainedSlice());
      in.skipBytes(in.readableBytes());
      return;
    }

    checkFrame(claimedUncompressedSize >= threshold, "Uncompressed size %s is less than"
            + " threshold %s", claimedUncompressedSize, threshold);
    int allowedMax = Math.min(claimedUncompressedSize, HARD_MAXIMUM_UNCOMPRESSED_SIZE);
    int initialCapacity = Math.min(claimedUncompressedSize, SOFT_MAXIMUM_UNCOMPRESSED_SIZE);

    ByteBuf compatibleIn = ensureCompatible(ctx.alloc(), compressor, in);
    ByteBuf uncompressed = preferredBuffer(ctx.alloc(), compressor, initialCapacity);
    try {
      compressor.inflate(compatibleIn, uncompressed, allowedMax);
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
