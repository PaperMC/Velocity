package com.velocitypowered.proxy.connection.http;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.util.ReferenceCountUtil;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;

class SimpleHttpResponseCollector extends ChannelInboundHandlerAdapter {
    private final StringBuilder buffer = new StringBuilder(1024);
    private final CompletableFuture<String> reply;

    SimpleHttpResponseCollector(CompletableFuture<String> reply) {
        this.reply = reply;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        try {
            if (msg instanceof HttpResponse) {
                HttpResponseStatus status = ((HttpResponse) msg).status();
                if (status != HttpResponseStatus.OK) {
                    ctx.close();
                    reply.completeExceptionally(new RuntimeException("Unexpected status code " + status.code()));
                }
            }

            if (msg instanceof HttpContent) {
                buffer.append(((HttpContent) msg).content().toString(StandardCharsets.UTF_8));

                if (msg instanceof LastHttpContent) {
                    ctx.close();
                    reply.complete(buffer.toString());
                }
            }
        } finally {
            ReferenceCountUtil.release(msg);
        }
    }
}
