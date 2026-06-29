/*
 * Copyright (C) 2018-2023 Velocity Contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.velocitypowered.proxy.network;

import com.google.common.base.Preconditions;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.velocitypowered.api.event.proxy.ListenerBoundEvent;
import com.velocitypowered.api.event.proxy.ListenerCloseEvent;
import com.velocitypowered.api.network.ListenerType;
import com.velocitypowered.natives.util.Natives;
import com.velocitypowered.proxy.VelocityServer;
import com.velocitypowered.proxy.network.netty.SeparatePoolInetNameResolver;
import com.velocitypowered.proxy.protocol.netty.GameSpyQueryHandler;
import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.WriteBufferWaterMark;
import io.netty.channel.unix.UnixChannelOption;
import io.netty.util.concurrent.GlobalEventExecutor;
import io.netty.util.concurrent.MultithreadEventExecutorGroup;
import java.net.InetSocketAddress;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Collection;
import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Manages endpoints managed by Velocity, along with initializing the Netty event loop group.
 */
public final class ConnectionManager {

  private static final WriteBufferWaterMark SERVER_WRITE_MARK = new WriteBufferWaterMark(1 << 20,
      1 << 21);
  private static final Logger LOGGER = LogManager.getLogger(ConnectionManager.class);
  private final Multimap<InetSocketAddress, Endpoint> endpoints = HashMultimap.create();
  private final TransportType transportType;
  private final VelocityServer server;
  // These are intentionally made public for plugins like ViaVersion, which inject their own
  // protocol logic into the proxy.
  @SuppressWarnings("WeakerAccess")
  public final ServerChannelInitializerHolder serverChannelInitializer;
  @SuppressWarnings("WeakerAccess")
  public final BackendChannelInitializerHolder backendChannelInitializer;

  // bVelocity: the event-loop groups and DNS resolver are created lazily on first use rather than
  // in the constructor. The constructor runs before bvelocity.toml is read (VelocityServer builds
  // this ConnectionManager before loading its configuration), so eager construction would force us
  // to use fixed defaults for the now-configurable event-loop / boss / DNS thread counts. Lazy
  // initialization defers creation until bind()/createWorker() are called, by which point the
  // bVelocity configuration is loaded.
  private volatile EventLoopGroup bossGroup;
  private volatile EventLoopGroup workerGroup;
  private volatile SeparatePoolInetNameResolver resolver;
  private volatile HttpClient sharedHttpClient;

  /**
   * Initializes the {@code ConnectionManager}.
   *
   * @param server a reference to the Velocity server
   */
  public ConnectionManager(VelocityServer server) {
    this.server = server;
    this.transportType = TransportType.bestType();
    this.serverChannelInitializer = new ServerChannelInitializerHolder(
        new ServerChannelInitializer(this.server));
    this.backendChannelInitializer = new BackendChannelInitializerHolder(
        new BackendChannelInitializer(this.server));
  }

  private EventLoopGroup bossGroup() {
    EventLoopGroup group = this.bossGroup;
    if (group == null) {
      synchronized (this) {
        group = this.bossGroup;
        if (group == null) {
          // bVelocity: respect the configured boss thread count (default 1). A single acceptor is
          // sufficient for non-REUSEPORT mode; REUSEPORT bypasses the boss group entirely.
          final int bossThreads = this.server.getBvConfiguration().getOptimization()
              .getBossThreads();
          group = this.transportType.createEventLoopGroup(TransportType.Type.BOSS, bossThreads);
          this.bossGroup = group;
        }
      }
    }
    return group;
  }

  private EventLoopGroup workerGroup() {
    EventLoopGroup group = this.workerGroup;
    if (group == null) {
      synchronized (this) {
        group = this.workerGroup;
        if (group == null) {
          // bVelocity: respect the configured worker thread count (0 = Netty default 2*CPU).
          final int eventLoopThreads = this.server.getBvConfiguration().getOptimization()
              .getEventLoopThreads();
          group = this.transportType.createEventLoopGroup(TransportType.Type.WORKER, eventLoopThreads);
          this.workerGroup = group;
        }
      }
    }
    return group;
  }

  private SeparatePoolInetNameResolver resolver() {
    SeparatePoolInetNameResolver r = this.resolver;
    if (r == null) {
      synchronized (this) {
        r = this.resolver;
        if (r == null) {
          final var optimization = this.server.getBvConfiguration().getOptimization();
          r = new SeparatePoolInetNameResolver(
              GlobalEventExecutor.INSTANCE,
              optimization.getDnsResolverThreads(),
              optimization.getDnsCacheTtlSeconds(),
              optimization.getDnsNegativeCacheTtlSeconds());
          this.resolver = r;
        }
      }
    }
    return r;
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
        .channelFactory(this.transportType.serverSocketChannelFactory)
        .childOption(ChannelOption.WRITE_BUFFER_WATER_MARK, SERVER_WRITE_MARK)
        .childHandler(this.serverChannelInitializer.get())
        .childOption(ChannelOption.TCP_NODELAY, true)
        .childOption(ChannelOption.IP_TOS, 0x18)
        .localAddress(address);

    if (server.getConfiguration().useTcpFastOpen()) {
      bootstrap.option(ChannelOption.TCP_FASTOPEN, 3);
    }

    if (server.getConfiguration().isEnableReusePort()) {
      // We don't need a boss group, since each worker will bind to the socket
      bootstrap.option(UnixChannelOption.SO_REUSEPORT, true)
          .group(this.workerGroup());
    } else {
      bootstrap.group(this.bossGroup(), this.workerGroup());
    }

    final int binds = server.getConfiguration().isEnableReusePort()
        ? ((MultithreadEventExecutorGroup) this.workerGroup()).executorCount() : 1;

    for (int bind = 0; bind < binds; bind++) {
      // Wait for each bind to open. If we encounter any errors, don't try to bind again.
      int finalBind = bind;
      ChannelFuture f = bootstrap.bind()
          .addListener((ChannelFutureListener) future -> {
            final Channel channel = future.channel();
            if (future.isSuccess()) {
              this.endpoints.put(address, new Endpoint(channel, ListenerType.MINECRAFT));

              LOGGER.info("Listening on {}", channel.localAddress());

              if (finalBind == 0) {
                // Warn people with console access that HAProxy is in use, see PR: #1436
                if (this.server.getConfiguration().isProxyProtocol()) {
                  LOGGER.warn(
                      "Using HAProxy and listening on {}, please ensure this listener is adequately firewalled.",
                      channel.localAddress());
                }

                // Fire the proxy bound event after the socket is bound
                server.getEventManager().fireAndForget(
                    new ListenerBoundEvent(address, ListenerType.MINECRAFT));
              }
            } else {
              LOGGER.error("Can't bind to {}", address, future.cause());
            }
          });
      f.syncUninterruptibly();

      if (!f.isSuccess()) {
        break;
      }
    }
  }

  /**
   * Binds a GS4 listener to the specified {@code hostname} and {@code port}.
   *
   * @param hostname the hostname to bind to
   * @param port     the port to bind to
   */
  public void queryBind(final String hostname, final int port) {
    InetSocketAddress address = new InetSocketAddress(hostname, port);
    final Bootstrap bootstrap = new Bootstrap()
        .channelFactory(this.transportType.datagramChannelFactory)
        .group(this.workerGroup())
        .handler(new GameSpyQueryHandler(this.server))
        .localAddress(address);
    bootstrap.bind()
        .addListener((ChannelFutureListener) future -> {
          final Channel channel = future.channel();
          if (future.isSuccess()) {
            this.endpoints.put(address, new Endpoint(channel, ListenerType.QUERY));
            LOGGER.info("Listening for GS4 query on {}", channel.localAddress());

            // Fire the proxy bound event after the socket is bound
            server.getEventManager().fireAndForget(
                new ListenerBoundEvent(address, ListenerType.QUERY));
          } else {
            LOGGER.error("Can't bind to {}", bootstrap.config().localAddress(), future.cause());
          }
        });
  }

  /**
   * Creates a TCP {@link Bootstrap} using Velocity's event loops.
   *
   * @param group the event loop group to use. Use {@code null} for the default worker group.
   * @return a new {@link Bootstrap}
   */
  public Bootstrap createWorker(@Nullable EventLoopGroup group) {
    Bootstrap bootstrap = new Bootstrap()
        .channelFactory(this.transportType.socketChannelFactory)
        .option(ChannelOption.TCP_NODELAY, true)
        // bVelocity: mirror the client-facing water mark on the backend connection. The default
        // 32KiB/64KiB water mark is ~32x smaller than the 1MiB/2MiB front-end mark; with the
        // back-end channel tripping its high water mark long before the front-end would, the
        // AutoReadHolderHandler back-pressure loop never closes cleanly and introduces spurious
        // autoRead toggling under bursty traffic.
        .option(ChannelOption.WRITE_BUFFER_WATER_MARK, SERVER_WRITE_MARK)
        // bVelocity: symmetry with the client-facing listener. The back-end connection carries the
        // same class of traffic and benefits from the same low-delay/throughput ToS hint, and
        // SO_KEEPALIVE guards against NAT/firewall conntrack expiry silently dropping idle
        // long-lived back-end connections before the read-timeout notices.
        .option(ChannelOption.IP_TOS, 0x18)
        .option(ChannelOption.SO_KEEPALIVE, true)
        .option(ChannelOption.CONNECT_TIMEOUT_MILLIS,
            this.server.getConfiguration().getConnectTimeout())
        .group(group == null ? this.workerGroup() : group)
        .resolver(this.resolver().asGroup());
    if (server.getConfiguration().useTcpFastOpen()) {
      bootstrap.option(ChannelOption.TCP_FASTOPEN_CONNECT, true);
    }
    return bootstrap;
  }

  /**
   * Closes the specified {@code oldBind} endpoint.
   *
   * @param oldBind the endpoint to close
   */
  public void close(InetSocketAddress oldBind) {
    Collection<Endpoint> endpoints = this.endpoints.removeAll(oldBind);
    Preconditions.checkState(!endpoints.isEmpty(), "Endpoint was not registered");

    ListenerType type = endpoints.iterator().next().getType();

    // Fire proxy close event to notify plugins of socket close. We block since plugins
    // should have a chance to be notified before the server stops accepting connections.
    server.getEventManager().fire(new ListenerCloseEvent(oldBind, type)).join();

    for (Endpoint endpoint : endpoints) {
      Channel serverChannel = endpoint.getChannel();
      LOGGER.info("Closing endpoint {}", serverChannel.localAddress());
      serverChannel.close().syncUninterruptibly();
    }
  }

  /**
   * Closes all the currently registered endpoints.
   *
   * @param interrupt should closing forward interruptions
   */
  public void closeEndpoints(boolean interrupt) {
    for (final Map.Entry<InetSocketAddress, Collection<Endpoint>> entry : this.endpoints.asMap()
        .entrySet()) {
      final InetSocketAddress address = entry.getKey();
      final Collection<Endpoint> endpoints = entry.getValue();
      ListenerType type = endpoints.iterator().next().getType();

      // Fire proxy close event to notify plugins of socket close. We block since plugins
      // should have a chance to be notified before the server stops accepting connections.
      server.getEventManager().fire(new ListenerCloseEvent(address, type)).join();

      for (Endpoint endpoint : endpoints) {
        LOGGER.info("Closing endpoint {}", address);
        if (interrupt) {
          try {
            endpoint.getChannel().close().sync();
          } catch (final InterruptedException e) {
            LOGGER.info("Interrupted whilst closing endpoint", e);
            Thread.currentThread().interrupt();
          }
        } else {
          endpoint.getChannel().close().syncUninterruptibly();
        }
      }
    }
    this.endpoints.clear();
  }

  /**
   * Closes all endpoints.
   */
  public void shutdown() {
    this.closeEndpoints(true);

    final SeparatePoolInetNameResolver r = this.resolver;
    if (r != null) {
      r.shutdown();
    }
    final HttpClient httpClient = this.sharedHttpClient;
    if (httpClient != null) {
      httpClient.close();
    }
  }

  /**
   * Returns the boss event-loop group. Under SO_REUSEPORT no boss group is created during bind, so
   * the worker group is returned instead (see the comment inside for why).
   *
   * @return the boss event-loop group (or the worker group under SO_REUSEPORT)
   */
  public EventLoopGroup getBossGroup() {
    // bVelocity: under SO_REUSEPORT each worker binds its own listening socket, so no boss group is
    // created during bind(). The sole caller (VelocityServer#awaitProxyShutdown) only needs a group
    // whose terminationFuture never completes (to park the main thread until JVM exit). Returning
    // the worker group here avoids lazily materializing an otherwise-unused, never-shut-down boss
    // group — which would leak a non-daemon thread — and keeps the park behavior identical.
    if (this.server.getConfiguration().isEnableReusePort()) {
      return workerGroup();
    }
    return bossGroup();
  }

  public ServerChannelInitializerHolder getServerChannelInitializer() {
    return this.serverChannelInitializer;
  }

  @SuppressWarnings("checkstyle:MissingJavadocMethod")
  public HttpClient createHttpClient() {
    HttpClient client = this.sharedHttpClient;
    if (client == null) {
      synchronized (this) {
        client = this.sharedHttpClient;
        if (client == null) {
          // bVelocity: share a single HttpClient across all hasJoined authentications rather than
          // building (and tearing down) one per login. A shared client reuses its TLS context and
          // keep-alive connection pool across players, saving a TLS handshake (1-2 RTT) per login.
          // It still runs on the Netty worker group instead of the ForkJoinPool common pool.
          client = HttpClient.newBuilder()
              .executor(this.workerGroup())
              .connectTimeout(Duration.ofMillis(this.server.getConfiguration().getConnectTimeout()))
              .build();
          this.sharedHttpClient = client;
        }
      }
    }
    return client;
  }

  /**
   * Drops the cached {@link HttpClient} so the next {@link #createHttpClient()} rebuilds it with
   * the current configuration (notably connect-timeout, which is fixed at build time and otherwise
   * would survive a {@code velocity reload} unchanged).
   *
   * <p>Any in-flight request on the old client may be failed by the close.
   */
  public void rebuildHttpClient() {
    final HttpClient old;
    synchronized (this) {
      old = this.sharedHttpClient;
      this.sharedHttpClient = null;
    }
    if (old != null) {
      old.close();
    }
  }

  public BackendChannelInitializerHolder getBackendChannelInitializer() {
    return this.backendChannelInitializer;
  }
}
