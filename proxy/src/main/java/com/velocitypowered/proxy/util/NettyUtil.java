package com.velocitypowered.proxy.util;

import com.velocitypowered.proxy.network.Connections;
import com.velocitypowered.proxy.util.except.QuietPromiseException;
import io.netty.channel.*;
import io.netty.util.ReferenceCountUtil;

public final class NettyUtil {
    private static final DiscardHandler INSTANCE = new DiscardHandler();

    private NettyUtil() {
        throw new AssertionError();
    }

    /**
     * This is needed because {@link io.netty.handler.codec.ByteToMessageDecoder} will continue to read messages, even tho
     * {@link io.netty.handler.codec.ByteToMessageDecoder#setSingleDecode(boolean)} was set to true.
     */
    public static void insertDiscard(Channel channel) {
        ChannelPipeline pipeline = channel.pipeline();
        if (pipeline.first() != INSTANCE) {
            pipeline.addFirst(Connections.DISCARD, INSTANCE);
        }
    }

    @ChannelHandler.Sharable
    public static final class DiscardHandler extends ChannelOutboundHandlerAdapter {
        private static final QuietPromiseException EXCEPTION = new QuietPromiseException();

        @Override
        public void read(ChannelHandlerContext ctx) throws Exception { }

        @Override
        public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
            ReferenceCountUtil.release(msg);
            promise.setFailure(EXCEPTION);
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception { }
    }
}
