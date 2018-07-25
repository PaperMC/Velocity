package io.minimum.minecraft.velocity.protocol.netty;

import com.google.common.base.Preconditions;
import io.minimum.minecraft.velocity.protocol.ProtocolUtils;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;

import java.util.List;
import java.util.zip.Inflater;

public class MinecraftCompressDecoder extends MessageToMessageDecoder<ByteBuf> {
    private static final int MAXIMUM_INITIAL_BUFFER_SIZE = 65536; // 64KiB

    private final int threshold;

    public MinecraftCompressDecoder(int threshold) {
        this.threshold = threshold;
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf msg, List<Object> out) throws Exception {
        int uncompressedSize = ProtocolUtils.readVarInt(msg);
        if (uncompressedSize == 0) {
            // Strip the now-useless uncompressed size, this message is already uncompressed.
            out.add(msg.slice().retain());
            return;
        }

        ByteBuf uncompressed = ctx.alloc().buffer(Math.min(uncompressedSize, MAXIMUM_INITIAL_BUFFER_SIZE));
        try {
            byte[] compressed = new byte[msg.readableBytes()];
            msg.readBytes(compressed);
            Inflater inflater = new Inflater();
            inflater.setInput(compressed);

            byte[] decompressed = new byte[8192];
            while (!inflater.finished()) {
                int inflatedBytes = inflater.inflate(decompressed);
                uncompressed.writeBytes(decompressed, 0, inflatedBytes);
            }

            Preconditions.checkState(uncompressedSize == uncompressed.readableBytes(), "Mismatched compression sizes");
            out.add(uncompressed);
        } catch (Exception e) {
            // If something went wrong, rethrow the exception, but ensure we free our temporary buffer first.
            uncompressed.release();
            throw e;
        }
    }
}
