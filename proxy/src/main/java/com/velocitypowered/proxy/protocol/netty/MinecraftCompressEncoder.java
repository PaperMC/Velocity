package com.velocitypowered.proxy.protocol.netty;

import com.velocitypowered.proxy.protocol.ProtocolUtils;
import com.velocitypowered.natives.compression.VelocityCompressor;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
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

        // in other words, see if a plain 8KiB buffer fits us well
        ByteBuf compressedBuffer = ctx.alloc().buffer(8192);
        try {
            int uncompressed = msg.readableBytes();
            compressor.deflate(msg, compressedBuffer);
            ProtocolUtils.writeVarInt(out, uncompressed);
            out.writeBytes(compressedBuffer);
        } finally {
            compressedBuffer.release();
        }
    }

    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
        compressor.dispose();
    }
}
