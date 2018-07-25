package io.minimum.minecraft.velocity.protocol.netty;

import io.minimum.minecraft.velocity.protocol.ProtocolUtils;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

import java.util.List;

public class MinecraftVarintFrameDecoder extends ByteToMessageDecoder {
    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        if (in.readableBytes() < 1) {
            return;
        }

        in.markReaderIndex();
        int packetLength = ProtocolUtils.readVarInt(in);
        if (in.readableBytes() < packetLength) {
            in.resetReaderIndex();
            return;
        }

        out.add(in.slice(in.readerIndex(), packetLength).retain());
        in.skipBytes(packetLength);
    }
}
