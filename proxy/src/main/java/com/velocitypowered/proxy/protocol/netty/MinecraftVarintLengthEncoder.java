package com.velocitypowered.proxy.protocol.netty;

import com.velocitypowered.proxy.protocol.ProtocolUtils;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import io.netty.handler.codec.MessageToMessageEncoder;

import java.util.List;

@ChannelHandler.Sharable
public class MinecraftVarintLengthEncoder extends MessageToMessageEncoder<ByteBuf> {
    public static final MinecraftVarintLengthEncoder INSTANCE = new MinecraftVarintLengthEncoder();

    private MinecraftVarintLengthEncoder() { }

    @Override
    protected void encode(ChannelHandlerContext ctx, ByteBuf buf, List<Object> list) throws Exception {
        ByteBuf lengthBuf = ctx.alloc().buffer(5); // the maximum size of a varint
        ProtocolUtils.writeVarInt(lengthBuf, buf.readableBytes());
        list.add(lengthBuf);
        list.add(buf.retain());
    }
}
