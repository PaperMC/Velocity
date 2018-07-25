package io.minimum.minecraft.velocity.protocol.netty;

import io.minimum.minecraft.velocity.protocol.ProtocolUtils;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

import java.util.zip.Deflater;

public class MinecraftCompressEncoder extends MessageToByteEncoder<ByteBuf> {
    private final int threshold;

    public MinecraftCompressEncoder(int threshold) {
        this.threshold = threshold;
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, ByteBuf msg, ByteBuf out) throws Exception {
        if (msg.readableBytes() <= threshold) {
            System.out.println("not compressing packet of 0x" + msg.readableBytes() + " size");
            // Under the threshold, there is nothing to do.
            ProtocolUtils.writeVarInt(out, 0);
            out.writeBytes(msg);
            return;
        }

        System.out.println("compressing packet of 0x" + msg.readableBytes() + " size");
        Deflater deflater = new Deflater();
        byte[] buf = new byte[msg.readableBytes()];
        msg.readBytes(buf);
        deflater.setInput(buf);
        deflater.finish();

        ByteBuf compressedBuffer = ctx.alloc().buffer();
        try {
            byte[] deflated = new byte[8192];
            while (!deflater.finished()) {
                int bytes = deflater.deflate(deflated);
                compressedBuffer.writeBytes(deflated, 0, bytes);
            }
            ProtocolUtils.writeVarInt(out, buf.length);
            out.writeBytes(compressedBuffer);
        } finally {
            compressedBuffer.release();
        }
    }
}
