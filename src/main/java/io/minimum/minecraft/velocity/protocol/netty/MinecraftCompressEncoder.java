package io.minimum.minecraft.velocity.protocol.netty;

import io.minimum.minecraft.velocity.protocol.ProtocolUtils;
import io.minimum.minecraft.velocity.protocol.compression.VelocityCompressor;
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
        if (msg.readableBytes() <= threshold) {
            // Under the threshold, there is nothing to do.
            ProtocolUtils.writeVarInt(out, 0);
            out.writeBytes(msg);
            return;
        }

        ByteBuf compressedBuffer = ctx.alloc().buffer();
        try {
            int uncompressed = msg.readableBytes();
            compressor.deflate(msg, compressedBuffer);
            ProtocolUtils.writeVarInt(out, uncompressed);
            out.writeBytes(compressedBuffer);
        } finally {
            compressedBuffer.release();
        }
    }
}
