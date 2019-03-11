package com.velocitypowered.proxy.network.http;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.util.ReferenceCountUtil;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;

class SimpleHttpResponseCollector extends ChannelInboundHandlerAdapter {

  private final StringBuilder buffer = new StringBuilder();
  private final CompletableFuture<SimpleHttpResponse> reply;
  private int httpCode;

  SimpleHttpResponseCollector(CompletableFuture<SimpleHttpResponse> reply) {
    this.reply = reply;
  }

  @Override
  public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
    try {
      if (msg instanceof HttpResponse) {
        HttpResponse response = (HttpResponse) msg;
        HttpResponseStatus status = response.status();
        this.httpCode = status.code();
      }

      if (msg instanceof HttpContent) {
        buffer.append(((HttpContent) msg).content().toString(StandardCharsets.UTF_8));

        if (msg instanceof LastHttpContent) {
          ctx.close();
          reply.complete(new SimpleHttpResponse(httpCode, buffer.toString()));
        }
      }
    } finally {
      ReferenceCountUtil.release(msg);
    }
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
    ctx.close();
    reply.completeExceptionally(cause);
  }
}
