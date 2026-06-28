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

import com.velocitypowered.proxy.util.concurrent.VelocityNettyThreadFactory;
import io.netty.channel.ChannelFactory;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.IoHandlerFactory;
import io.netty.channel.MultiThreadIoEventLoopGroup;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollDatagramChannel;
import io.netty.channel.epoll.EpollIoHandler;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.epoll.EpollSocketChannel;
import io.netty.channel.kqueue.KQueue;
import io.netty.channel.kqueue.KQueueDatagramChannel;
import io.netty.channel.kqueue.KQueueIoHandler;
import io.netty.channel.kqueue.KQueueServerSocketChannel;
import io.netty.channel.kqueue.KQueueSocketChannel;
import io.netty.channel.nio.NioIoHandler;
import io.netty.channel.socket.DatagramChannel;
import io.netty.channel.socket.ServerSocketChannel;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.channel.uring.IoUring;
import io.netty.channel.uring.IoUringDatagramChannel;
import io.netty.channel.uring.IoUringIoHandler;
import io.netty.channel.uring.IoUringServerSocketChannel;
import io.netty.channel.uring.IoUringSocketChannel;
import java.util.concurrent.ThreadFactory;
import java.util.function.Supplier;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Enumerates the supported transports for Velocity.
 */
public enum TransportType {
  NIO("NIO", NioServerSocketChannel::new,
      NioSocketChannel::new,
      NioDatagramChannel::new,
      NioIoHandler::newFactory),
  EPOLL("epoll", EpollServerSocketChannel::new,
      EpollSocketChannel::new,
      EpollDatagramChannel::new,
      EpollIoHandler::newFactory),
  KQUEUE("kqueue", KQueueServerSocketChannel::new,
      KQueueSocketChannel::new,
      KQueueDatagramChannel::new,
      KQueueIoHandler::newFactory),
  IO_URING("io_uring", IoUringServerSocketChannel::new,
      IoUringSocketChannel::new,
      IoUringDatagramChannel::new,
      IoUringIoHandler::newFactory);

  final String name;
  final ChannelFactory<? extends ServerSocketChannel> serverSocketChannelFactory;
  final ChannelFactory<? extends SocketChannel> socketChannelFactory;
  final ChannelFactory<? extends DatagramChannel> datagramChannelFactory;
  final Supplier<IoHandlerFactory> ioHandlerFactorySupplier;

  TransportType(final String name,
      final ChannelFactory<? extends ServerSocketChannel> serverSocketChannelFactory,
      final ChannelFactory<? extends SocketChannel> socketChannelFactory,
      final ChannelFactory<? extends DatagramChannel> datagramChannelFactory,
      final Supplier<IoHandlerFactory> ioHandlerFactorySupplier) {
    this.name = name;
    this.serverSocketChannelFactory = serverSocketChannelFactory;
    this.socketChannelFactory = socketChannelFactory;
    this.datagramChannelFactory = datagramChannelFactory;
    this.ioHandlerFactorySupplier = ioHandlerFactorySupplier;
  }

  @Override
  public String toString() {
    return this.name;
  }

  private static final Logger LOGGER = LogManager.getLogger(TransportType.class);

  /**
   * Creates a new event loop group for the given type.
   *
   * @param type the type of event loop group to create
   * @param threadCount the number of threads to use, or {@code 0} for Netty's default ({@code 2 * cpu})
   * @return the event loop group
   */
  public EventLoopGroup createEventLoopGroup(final Type type, final int threadCount) {
    return new MultiThreadIoEventLoopGroup(
        threadCount, createThreadFactory(this.name, type), this.ioHandlerFactorySupplier.get());
  }

  /**
   * Creates a new event loop group for the given type using Netty's default thread count.
   *
   * @param type the type of event loop group to create
   * @return the event loop group
   */
  public EventLoopGroup createEventLoopGroup(final Type type) {
    return createEventLoopGroup(type, 0);
  }

  private static ThreadFactory createThreadFactory(final String name, final Type type) {
    return new VelocityNettyThreadFactory("Netty " + name + ' ' + type.toString() + " #%d");
  }

  /**
   * Determines the "best" transport to initialize.
   *
   * <p>bVelocity: when a native transport is requested but unavailable, log the underlying cause so
   * operators can distinguish a silent NIO fallback (an order-of-magnitude performance regression)
   * from a genuine business problem.
   *
   * @return the transport to use
   */
  public static TransportType bestType() {
    if (Boolean.getBoolean("velocity.disable-native-transport")) {
      return NIO;
    }

    final boolean iouringRequested = Boolean.getBoolean("velocity.enable-iouring-transport");
    if (iouringRequested && !IoUring.isAvailable()) {
      LOGGER.warn("io_uring transport was requested (-Dvelocity.enable-iouring-transport) but is "
          + "unavailable; falling back. Cause: {}", IoUring.unavailabilityCause());
    } else if (IoUring.isAvailable() && iouringRequested) {
      return IO_URING;
    }

    if (Epoll.isAvailable()) {
      return EPOLL;
    }
    LOGGER.warn("Epoll native transport is unavailable, falling back to NIO. Cause: {}",
        Epoll.unavailabilityCause());

    if (KQueue.isAvailable()) {
      return KQUEUE;
    }

    return NIO;
  }

  /**
   * Event loop group types.
   */
  public enum Type {
    /**
     * Accepts connections and distributes them to workers.
     */
    BOSS("Boss"),
    /**
     * Thread that handles connections.
     */
    WORKER("Worker");

    private final String name;

    Type(final String name) {
      this.name = name;
    }

    @Override
    public String toString() {
      return this.name;
    }
  }
}
