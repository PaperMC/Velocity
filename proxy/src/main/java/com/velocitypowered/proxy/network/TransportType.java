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
import io.netty.channel.Channel;
import io.netty.channel.ChannelFactory;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.IoHandlerFactory;
import io.netty.channel.MultiThreadIoEventLoopGroup;
import io.netty.channel.ServerChannel;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollDatagramChannel;
import io.netty.channel.epoll.EpollDomainSocketChannel;
import io.netty.channel.epoll.EpollIoHandler;
import io.netty.channel.epoll.EpollServerDomainSocketChannel;
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
import io.netty.channel.unix.DomainSocketAddress;
import io.netty.channel.unix.DomainSocketChannel;
import io.netty.channel.unix.ServerDomainSocketChannel;
import io.netty.channel.uring.IoUring;
import io.netty.channel.uring.IoUringDatagramChannel;
import io.netty.channel.uring.IoUringDomainSocketChannel;
import io.netty.channel.uring.IoUringIoHandler;
import io.netty.channel.uring.IoUringServerDomainSocketChannel;
import io.netty.channel.uring.IoUringServerSocketChannel;
import io.netty.channel.uring.IoUringSocketChannel;
import java.net.SocketAddress;
import java.util.concurrent.ThreadFactory;
import java.util.function.Supplier;

/**
 * Enumerates the supported transports for Velocity.
 */
public enum TransportType {
  NIO("NIO", NioServerSocketChannel::new,
      NioSocketChannel::new,
      NioDatagramChannel::new,
      null,
      null,
      NioIoHandler::newFactory),
  EPOLL("epoll", EpollServerSocketChannel::new,
      EpollSocketChannel::new,
      EpollDatagramChannel::new,
      EpollServerDomainSocketChannel::new,
      EpollDomainSocketChannel::new,
      EpollIoHandler::newFactory),
  KQUEUE("kqueue", KQueueServerSocketChannel::new,
      KQueueSocketChannel::new,
      KQueueDatagramChannel::new,
      null,
      null,
      KQueueIoHandler::newFactory),
  IO_URING("io_uring", IoUringServerSocketChannel::new,
      IoUringSocketChannel::new,
      IoUringDatagramChannel::new,
      IoUringServerDomainSocketChannel::new,
      IoUringDomainSocketChannel::new,
      IoUringIoHandler::newFactory);

  final String name;
  final ChannelFactory<? extends ServerSocketChannel> serverSocketChannelFactory;
  final ChannelFactory<? extends SocketChannel> socketChannelFactory;
  final ChannelFactory<? extends DatagramChannel> datagramChannelFactory;
  final ChannelFactory<? extends ServerDomainSocketChannel> domainServerSocketChannelFactory;
  final ChannelFactory<? extends DomainSocketChannel> domainSocketChannelFactory;
  final Supplier<IoHandlerFactory> ioHandlerFactorySupplier;

  TransportType(final String name,
      final ChannelFactory<? extends ServerSocketChannel> serverSocketChannelFactory,
      final ChannelFactory<? extends SocketChannel> socketChannelFactory,
      final ChannelFactory<? extends DatagramChannel> datagramChannelFactory,
      final ChannelFactory<? extends ServerDomainSocketChannel> domainServerSocketChannelFactory,
      final ChannelFactory<? extends DomainSocketChannel> domainSocketChannelFactory,
      final Supplier<IoHandlerFactory> ioHandlerFactorySupplier) {
    this.name = name;
    this.serverSocketChannelFactory = serverSocketChannelFactory;
    this.socketChannelFactory = socketChannelFactory;
    this.datagramChannelFactory = datagramChannelFactory;
    this.domainServerSocketChannelFactory = domainServerSocketChannelFactory;
    this.domainSocketChannelFactory = domainSocketChannelFactory;
    this.ioHandlerFactorySupplier = ioHandlerFactorySupplier;
  }

  @Override
  public String toString() {
    return this.name;
  }

  /**
   * Returns the channel factory to use to listen on the specified socket address.
   *
   * @param address the address we want to listen on
   * @return the channel factory
   */
  public ChannelFactory<? extends ServerChannel> getServerChannelFactory(SocketAddress address) {
    if (address instanceof DomainSocketAddress) {
      if (this.domainServerSocketChannelFactory == null) {
        throw new IllegalArgumentException(
                "Domain sockets are not available for non-Linux platforms");
      }
      return this.domainServerSocketChannelFactory;
    }
    return this.serverSocketChannelFactory;
  }

  /**
   * Returns the channel factory to use to connect on the specified socket address.
   *
   * @param address the address we want to connect to
   * @return the channel factory
   */
  public ChannelFactory<? extends Channel> getClientChannelFactory(SocketAddress address) {
    if (address instanceof DomainSocketAddress) {
      if (this.domainSocketChannelFactory == null) {
        throw new IllegalArgumentException(
                "Domain sockets are not available for non-Linux platforms");
      }
      return this.domainSocketChannelFactory;
    }
    return this.socketChannelFactory;
  }

  /**
   * Creates a new event loop group for the given type.
   *
   * @param type the type of event loop group to create
   * @return the event loop group
   */
  public EventLoopGroup createEventLoopGroup(final Type type) {
    return new MultiThreadIoEventLoopGroup(
        0, createThreadFactory(this.name, type), this.ioHandlerFactorySupplier.get());
  }

  private static ThreadFactory createThreadFactory(final String name, final Type type) {
    return new VelocityNettyThreadFactory("Netty " + name + ' ' + type.toString() + " #%d");
  }

  /**
   * Determines the "best" transport to initialize.
   *
   * @return the transport to use
   */
  public static TransportType bestType() {
    if (Boolean.getBoolean("velocity.disable-native-transport")) {
      return NIO;
    }

    if (IoUring.isAvailable() && Boolean.getBoolean("velocity.enable-iouring-transport")) {
      return IO_URING;
    }

    if (Epoll.isAvailable()) {
      return EPOLL;
    }

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
