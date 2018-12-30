package com.velocitypowered.proxy.network.http;

import com.google.common.base.VerifyException;
import com.velocitypowered.proxy.VelocityServer;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.pool.AbstractChannelPoolMap;
import io.netty.channel.pool.ChannelPoolHandler;
import io.netty.channel.pool.ChannelPoolMap;
import io.netty.channel.pool.FixedChannelPool;
import io.netty.channel.pool.SimpleChannelPool;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslHandler;
import java.net.InetSocketAddress;
import java.net.URL;
import java.util.concurrent.CompletableFuture;
import javax.net.ssl.SSLEngine;

public class NettyHttpClient {

  private final ChannelPoolMap<InetSocketAddress, SimpleChannelPool> poolMap;
  private final String userAgent;

  /**
   * Initializes the HTTP client.
   *
   * @param server the Velocity server
   */
  public NettyHttpClient(VelocityServer server) {
    this.userAgent = server.getVersion().getName() + "/" + server.getVersion().getVersion();
    Bootstrap bootstrap = server.initializeGenericBootstrap();
    this.poolMap = new AbstractChannelPoolMap<InetSocketAddress, SimpleChannelPool>() {
      @Override
      protected SimpleChannelPool newPool(InetSocketAddress key) {
        return new FixedChannelPool(bootstrap.remoteAddress(key), new ChannelPoolHandler() {
          @Override
          public void channelReleased(Channel channel) throws Exception {
            channel.pipeline().remove("collector");
          }

          @Override
          public void channelAcquired(Channel channel) throws Exception {
            // We don't do anything special when acquiring channels. The channel handler cleans up
            // after each connection is used.
          }

          @Override
          public void channelCreated(Channel channel) throws Exception {
            if (key.getPort() == 443) {
              SslContext context = SslContextBuilder.forClient().build();
              SSLEngine engine = context.newEngine(channel.alloc());
              channel.pipeline().addLast("ssl", new SslHandler(engine));
            }
            channel.pipeline().addLast("http", new HttpClientCodec());
          }
        }, 8);
      }
    };
  }

  /**
   * Attempts an HTTP GET request to the specified URL.
   * @param url the URL to fetch
   * @return a future representing the response
   */
  public CompletableFuture<SimpleHttpResponse> get(URL url) {
    String host = url.getHost();
    int port = url.getPort();
    boolean ssl = url.getProtocol().equals("https");
    if (port == -1) {
      port = ssl ? 443 : 80;
    }

    CompletableFuture<SimpleHttpResponse> reply = new CompletableFuture<>();
    InetSocketAddress address = new InetSocketAddress(host, port);
    poolMap.get(address)
        .acquire()
        .addListener(future -> {
          if (future.isSuccess()) {
            Channel channel = (Channel) future.getNow();
            if (channel == null) {
              throw new VerifyException("Null channel retrieved from pool!");
            }
            channel.pipeline().addLast("collector", new SimpleHttpResponseCollector(reply));

            DefaultFullHttpRequest request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1,
                HttpMethod.GET, url.getPath() + "?" + url.getQuery());
            request.headers().add(HttpHeaderNames.HOST, url.getHost());
            request.headers().add(HttpHeaderNames.USER_AGENT, userAgent);
            channel.writeAndFlush(request);

            // Make sure to release this connection
            reply.whenComplete((resp, err) -> poolMap.get(address).release(channel));
          } else {
            reply.completeExceptionally(future.cause());
          }
        });
    return reply;
  }
}
