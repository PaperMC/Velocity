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

  /**
   * Initializes the {@code ConnectionManager}.
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
          .group(this.workerGroup);
    } else {
      bootstrap.group(this.bossGroup, this.workerGroup);
    }

    final int binds = server.getConfiguration().isEnableReusePort()
        ? ((MultithreadEventExecutorGroup) this.workerGroup).executorCount() : 1;

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
        .group(this.workerGroup)
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
        .option(ChannelOption.CONNECT_TIMEOUT_MILLIS,
            this.server.getConfiguration().getConnectTimeout())
        .group(group == null ? this.workerGroup : group)
        .resolver(this.resolver.asGroup());
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

    this.resolver.shutdown();
  }

  public EventLoopGroup getBossGroup() {
    return bossGroup;
  }

  public ServerChannelInitializerHolder getServerChannelInitializer() {
    return this.serverChannelInitializer;
  }

  @SuppressWarnings("checkstyle:MissingJavadocMethod")
  public HttpClient createHttpClient() {
    return HttpClient.newBuilder()
            .executor(this.workerGroup)
            .build();
  }

  public BackendChannelInitializerHolder getBackendChannelInitializer() {
    return this.backendChannelInitializer;
  }
}
