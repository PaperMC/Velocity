package com.velocitypowered.proxy.network;

import static org.asynchttpclient.Dsl.asyncHttpClient;
import static org.asynchttpclient.Dsl.config;

import com.google.common.base.Preconditions;
import com.velocitypowered.natives.util.Natives;
import com.velocitypowered.proxy.VelocityServer;
import com.velocitypowered.proxy.network.netty.SeparatePoolInetNameResolver;
import com.velocitypowered.proxy.protocol.netty.GS4QueryHandler;
import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.WriteBufferWaterMark;
import io.netty.channel.epoll.EpollChannelOption;
import io.netty.resolver.dns.DnsAddressResolverGroup;
import io.netty.resolver.dns.DnsNameResolverBuilder;
import io.netty.util.concurrent.GlobalEventExecutor;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.RequestBuilder;
import org.asynchttpclient.filter.FilterContext;
import org.asynchttpclient.filter.FilterContext.FilterContextBuilder;
import org.asynchttpclient.filter.FilterException;
import org.asynchttpclient.filter.RequestFilter;
import org.checkerframework.checker.nullness.qual.Nullable;

public final class ConnectionManager {

  private static final WriteBufferWaterMark SERVER_WRITE_MARK = new WriteBufferWaterMark(1 << 20,
      1 << 21);
  private static final Logger LOGGER = LogManager.getLogger(ConnectionManager.class);
  private final Map<InetSocketAddress, Channel> endpoints = new HashMap<>();
  private final TransportType transportType;
  private final EventLoopGroup bossGroup;
  private final EventLoopGroup workerGroup;
  private final VelocityServer server;
  // These are intentionally made public for plugins like ViaVersion, which inject their own
  // protocol logic into the proxy.
  @SuppressWarnings("WeakerAccess")
  public final ServerChannelInitializerHolder serverChannelInitializer;
  @SuppressWarnings("WeakerAccess")
  public final BackendChannelInitializerHolder backendChannelInitializer;

  private final SeparatePoolInetNameResolver resolver;
  private final AsyncHttpClient httpClient;

  /**
   * Initalizes the {@code ConnectionManager}.
   *
   * @param server a reference to the Velocity server
   */
  public ConnectionManager(VelocityServer server) {
    this.server = server;
    this.transportType = TransportType.bestType();
    this.bossGroup = this.transportType.createEventLoopGroup(TransportType.Type.BOSS);
    this.workerGroup = this.transportType.createEventLoopGroup(TransportType.Type.WORKER);
    this.serverChannelInitializer = new ServerChannelInitializerHolder(
        new ServerChannelInitializer(this.server));
    this.backendChannelInitializer = new BackendChannelInitializerHolder(
        new BackendChannelInitializer(this.server));
    this.resolver = new SeparatePoolInetNameResolver(GlobalEventExecutor.INSTANCE);
    this.httpClient = asyncHttpClient(config()
        .setEventLoopGroup(this.workerGroup)
        .setUserAgent(server.getVersion().getName() + "/" + server.getVersion().getVersion())
        .addRequestFilter(new RequestFilter() {
          @Override
          public <T> FilterContext<T> filter(FilterContext<T> ctx) {
            return new FilterContextBuilder<>(ctx)
                .request(new RequestBuilder(ctx.getRequest())
                    .setNameResolver(resolver)
                    .build())
                .build();
          }
        })
        .build());
  }

  public void logChannelInformation() {
    LOGGER.info("Connections will use {} channels, {} compression, {} ciphers", this.transportType,
        Natives.compress.getLoadedVariant(), Natives.cipher.getLoadedVariant());
  }

  /**
   * Binds a Minecraft listener to the specified {@code address}.
   *
   * @param address the address to bind to
   */
  public void bind(final InetSocketAddress address) {
    final ServerBootstrap bootstrap = new ServerBootstrap()
        .channel(this.transportType.serverSocketChannelClass)
        .group(this.bossGroup, this.workerGroup)
        .childOption(ChannelOption.WRITE_BUFFER_WATER_MARK, SERVER_WRITE_MARK)
        .childHandler(this.serverChannelInitializer.get())
        .childOption(ChannelOption.TCP_NODELAY, true)
        .childOption(ChannelOption.IP_TOS, 0x18)
        .localAddress(address);

    if (transportType == TransportType.EPOLL && server.getConfiguration().useTcpFastOpen()) {
      bootstrap.option(EpollChannelOption.TCP_FASTOPEN, 3);
    }

    bootstrap.bind()
        .addListener((ChannelFutureListener) future -> {
          final Channel channel = future.channel();
          if (future.isSuccess()) {
            this.endpoints.put(address, channel);
            LOGGER.info("Listening on {}", channel.localAddress());
          } else {
            LOGGER.error("Can't bind to {}", address, future.cause());
          }
        });
  }

  /**
   * Binds a GS4 listener to the specified {@code hostname} and {@code port}.
   *
   * @param hostname the hostname to bind to
   * @param port the port to bind to
   */
  public void queryBind(final String hostname, final int port) {
    InetSocketAddress address = new InetSocketAddress(hostname, port);
    final Bootstrap bootstrap = new Bootstrap()
        .channel(this.transportType.datagramChannelClass)
        .group(this.workerGroup)
        .handler(new GS4QueryHandler(this.server))
        .localAddress(address);
    bootstrap.bind()
        .addListener((ChannelFutureListener) future -> {
          final Channel channel = future.channel();
          if (future.isSuccess()) {
            this.endpoints.put(address, channel);
            LOGGER.info("Listening for GS4 query on {}", channel.localAddress());
          } else {
            LOGGER.error("Can't bind to {}", bootstrap.config().localAddress(), future.cause());
          }
        });
  }

  /**
   * Creates a TCP {@link Bootstrap} using Velocity's event loops.
   *
   * @param group the event loop group to use. Use {@code null} for the default worker group.
   *
   * @return a new {@link Bootstrap}
   */
  public Bootstrap createWorker(@Nullable EventLoopGroup group) {
    Bootstrap bootstrap = new Bootstrap()
        .channel(this.transportType.socketChannelClass)
        .option(ChannelOption.TCP_NODELAY, true)
        .option(ChannelOption.CONNECT_TIMEOUT_MILLIS,
            this.server.getConfiguration().getConnectTimeout())
        .group(group == null ? this.workerGroup : group)
        .resolver(this.resolver.asGroup());
    if (transportType == TransportType.EPOLL && server.getConfiguration().useTcpFastOpen()) {
      bootstrap.option(EpollChannelOption.TCP_FASTOPEN_CONNECT, true);
    }
    return bootstrap;
  }

  /**
   * Closes the specified {@code oldBind} endpoint.
   *
   * @param oldBind the endpoint to close
   */
  public void close(InetSocketAddress oldBind) {
    Channel serverChannel = endpoints.remove(oldBind);
    Preconditions.checkState(serverChannel != null, "Endpoint %s not registered", oldBind);
    LOGGER.info("Closing endpoint {}", serverChannel.localAddress());
    serverChannel.close().syncUninterruptibly();
  }

  /**
   * Closes all endpoints.
   */
  public void shutdown() {
    for (final Channel endpoint : this.endpoints.values()) {
      try {
        LOGGER.info("Closing endpoint {}", endpoint.localAddress());
        endpoint.close().sync();
      } catch (final InterruptedException e) {
        LOGGER.info("Interrupted whilst closing endpoint", e);
        Thread.currentThread().interrupt();
      }
    }

    this.resolver.shutdown();
  }

  public EventLoopGroup getBossGroup() {
    return bossGroup;
  }

  public ServerChannelInitializerHolder getServerChannelInitializer() {
    return this.serverChannelInitializer;
  }

  public AsyncHttpClient getHttpClient() {
    return httpClient;
  }

  public BackendChannelInitializerHolder getBackendChannelInitializer() {
    return this.backendChannelInitializer;
  }
}
