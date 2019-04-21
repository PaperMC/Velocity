package com.velocitypowered.proxy.network.http;

import com.velocitypowered.proxy.VelocityServer;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoop;
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
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import javax.net.ssl.SSLEngine;

public class NettyHttpClient {

  private final String userAgent;
  private final VelocityServer server;

  /**
   * Initializes the HTTP client.
   *
   * @param server the Velocity server
   */
  public NettyHttpClient(VelocityServer server) {
    this.userAgent = server.getVersion().getName() + "/" + server.getVersion().getVersion();
    this.server = server;
  }

  /**
   * Attempts an HTTP GET request to the specified URL.
   * @param url the URL to fetch
   * @param loop the event loop to use
   * @return a future representing the response
   */
  public CompletableFuture<SimpleHttpResponse> get(URL url, EventLoop loop) {
    String host = url.getHost();
    int port = url.getPort();
    boolean ssl = url.getProtocol().equals("https");
    if (port == -1) {
      port = ssl ? 443 : 80;
    }

    InetSocketAddress address = InetSocketAddress.createUnresolved(host, port);
    CompletableFuture<SimpleHttpResponse> reply = new CompletableFuture<>();
    server.initializeGenericBootstrap(loop)
        .handler(new ChannelInitializer<Channel>() {
          @Override
          protected void initChannel(Channel ch) throws Exception {
            if (ssl) {
              SslContext context = SslContextBuilder.forClient().protocols("TLSv1.2").build();
              // Unbelievably, Java doesn't automatically check the CN to make sure we're talking
              // to the right host! Therefore, we provide the intended host name and port, along
              // with asking Java very nicely if it could check the hostname in the certificate
              // for us.
              SSLEngine engine = context.newEngine(ch.alloc(), address.getHostString(),
                  address.getPort());
              engine.getSSLParameters().setEndpointIdentificationAlgorithm("HTTPS");
              ch.pipeline().addLast("ssl", new SslHandler(engine));
            }
            ch.pipeline().addLast("http", new HttpClientCodec());
            ch.pipeline().addLast("collector", new SimpleHttpResponseCollector(reply));
          }
        })
        .connect(address)
        .addListener((ChannelFutureListener) future -> {
          if (future.isSuccess()) {
            Channel channel = future.channel();

            DefaultFullHttpRequest request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1,
                HttpMethod.GET, url.getPath() + "?" + url.getQuery());
            request.headers().add(HttpHeaderNames.HOST, url.getHost());
            request.headers().add(HttpHeaderNames.USER_AGENT, userAgent);
            channel.writeAndFlush(request, channel.voidPromise());
          } else {
            reply.completeExceptionally(future.cause());
          }
        });
    return reply;
  }
}
