package io.minimum.minecraft.velocity.protocol.netty;

import io.minimum.minecraft.velocity.protocol.ProtocolUtils;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

@ChannelHandler.Sharable
public class MinecraftVarintLengthEncoder extends MessageToByteEncoder<ByteBuf> {
    public static final MinecraftVarintLengthEncoder INSTANCE = new MinecraftVarintLengthEncoder();

    private MinecraftVarintLengthEncoder() { }

    @Override
    protected void encode(ChannelHandlerContext ctx, ByteBuf msg, ByteBuf out) throws Exception {
        ProtocolUtils.writeVarInt(out, msg.readableBytes());
        out.writeBytes(msg);
    }
}
