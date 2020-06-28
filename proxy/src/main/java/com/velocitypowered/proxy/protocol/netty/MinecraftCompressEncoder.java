package com.velocitypowered.proxy.protocol.netty;

import com.velocitypowered.natives.compression.VelocityCompressor;
import com.velocitypowered.natives.util.MoreByteBufUtils;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

public class MinecraftCompressEncoder extends MessageToByteEncoder<ByteBuf> {

  private final int threshold;
  private final VelocityCompressor compressor;

  public MinecraftCompressEncoder(int threshold, VelocityCompressor compressor) {
    this.threshold = threshold;
    this.compressor = compressor;
  }

  @Override
  protected void encode(ChannelHandlerContext ctx, ByteBuf msg, ByteBuf out) throws Exception {
    int uncompressed = msg.readableBytes();
    if (uncompressed <= threshold) {
      // Under the threshold, there is nothing to do.
      ProtocolUtils.writeVarInt(out, 0);
      out.writeBytes(msg);
    } else {
      ProtocolUtils.writeVarInt(out, uncompressed);
      ByteBuf compatibleIn = MoreByteBufUtils.ensureCompatible(ctx.alloc(), compressor, msg);
      try {
        compressor.deflate(compatibleIn, out);
      } finally {
        compatibleIn.release();
      }
    }
  }

  @Override
  protected ByteBuf allocateBuffer(ChannelHandlerContext ctx, ByteBuf msg, boolean preferDirect)
      throws Exception {
    // Follow the advice of https://github.com/ebiggers/libdeflate/blob/master/libdeflate.h#L103
    // here for compression. The maximum buffer size if the data compresses well (which is almost
    // always the case) is one less the input buffer.
    int initialBufferSize = msg.readableBytes() + 1;
    return MoreByteBufUtils.preferredBuffer(ctx.alloc(), compressor, initialBufferSize);
  }

  @Override
  public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
    compressor.dispose();
  }
}
