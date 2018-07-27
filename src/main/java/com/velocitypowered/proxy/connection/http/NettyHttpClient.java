package com.velocitypowered.proxy.connection.http;

import com.velocitypowered.proxy.VelocityServer;
import io.netty.channel.*;
import io.netty.handler.codec.http.*;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslHandler;

import javax.net.ssl.SSLEngine;
import java.net.URL;
import java.util.concurrent.CompletableFuture;

public class NettyHttpClient {
    private final VelocityServer server;

    public NettyHttpClient(VelocityServer server) {
        this.server = server;
    }

    public CompletableFuture<String> get(URL url) {
        String host = url.getHost();
        int port = url.getPort();
        boolean ssl = url.getProtocol().equals("https");
        if (port == -1) {
            port = ssl ? 443 : 80;
        }

        CompletableFuture<String> reply = new CompletableFuture<>();
        server.initializeGenericBootstrap()
                .handler(new ChannelInitializer<Channel>() {
                    @Override
                    protected void initChannel(Channel ch) throws Exception {
                        if (ssl) {
                            SslContext context = SslContextBuilder.forClient().build();
                            SSLEngine engine = context.newEngine(ch.alloc());
                            ch.pipeline().addLast(new SslHandler(engine));
                        }
                        ch.pipeline().addLast(new HttpClientCodec());
                        ch.pipeline().addLast(new ChannelInboundHandlerAdapter() {
                            @Override
                            public void channelActive(ChannelHandlerContext ctx) throws Exception {
                                DefaultFullHttpRequest request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, url.getPath() + "?" + url.getQuery());
                                request.headers().add(HttpHeaderNames.HOST, url.getHost());
                                request.headers().add(HttpHeaderNames.USER_AGENT, "Velocity");
                                ctx.writeAndFlush(request);
                            }
                        });
                        ch.pipeline().addLast(new SimpleHttpResponseCollector(reply));
                    }
                })
                .connect(host, port)
                .addListener(new ChannelFutureListener() {
                    @Override
                    public void operationComplete(ChannelFuture future) throws Exception {
                        if (!future.isSuccess()) {
                            reply.completeExceptionally(future.cause());
                        }
                    }
                });
        return reply;
    }
}
