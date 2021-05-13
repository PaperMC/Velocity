/*
 * Copyright (C) 2018 Velocity Contributors
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
import io.netty.channel.ServerChannel;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollDatagramChannel;
import io.netty.channel.epoll.EpollDomainSocketChannel;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerDomainSocketChannel;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.epoll.EpollSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.DatagramChannel;
import io.netty.channel.socket.ServerSocketChannel;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.channel.unix.DomainSocketAddress;
import io.netty.channel.unix.DomainSocketChannel;
import io.netty.channel.unix.ServerDomainSocketChannel;
import java.net.SocketAddress;
import java.util.concurrent.ThreadFactory;
import java.util.function.BiFunction;
import org.checkerframework.checker.nullness.qual.Nullable;

enum TransportType {
  NIO("NIO", NioServerSocketChannel::new,
      NioSocketChannel::new,
      NioDatagramChannel::new,
      null,
      null,
      (name, type) -> new NioEventLoopGroup(0, createThreadFactory(name, type))),
  EPOLL("epoll", EpollServerSocketChannel::new,
      EpollSocketChannel::new,
      EpollDatagramChannel::new,
      EpollServerDomainSocketChannel::new,
      EpollDomainSocketChannel::new,
      (name, type) -> new EpollEventLoopGroup(0, createThreadFactory(name, type)));

  final String name;
  final ChannelFactory<? extends ServerSocketChannel> serverSocketChannelFactory;
  final ChannelFactory<? extends SocketChannel> socketChannelFactory;
  final ChannelFactory<? extends DatagramChannel> datagramChannelFactory;
  final @Nullable ChannelFactory<? extends ServerDomainSocketChannel> domainServerSocketChannelFactory;
  final @Nullable ChannelFactory<? extends DomainSocketChannel> domainSocketChannelFactory;
  final BiFunction<String, Type, EventLoopGroup> eventLoopGroupFactory;

  TransportType(final String name,
      final ChannelFactory<? extends ServerSocketChannel> serverSocketChannelFactory,
      final ChannelFactory<? extends SocketChannel> socketChannelFactory,
      final ChannelFactory<? extends DatagramChannel> datagramChannelFactory,
      @Nullable final ChannelFactory<? extends ServerDomainSocketChannel> domainServerSocketChannelFactory,
      @Nullable final ChannelFactory<? extends DomainSocketChannel> domainSocketChannelFactory,
      final BiFunction<String, Type, EventLoopGroup> eventLoopGroupFactory) {
    this.name = name;
    this.serverSocketChannelFactory = serverSocketChannelFactory;
    this.socketChannelFactory = socketChannelFactory;
    this.datagramChannelFactory = datagramChannelFactory;
    this.domainServerSocketChannelFactory = domainServerSocketChannelFactory;
    this.domainSocketChannelFactory = domainSocketChannelFactory;
    this.eventLoopGroupFactory = eventLoopGroupFactory;
  }

  @Override
  public String toString() {
    return this.name;
  }

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

  public EventLoopGroup createEventLoopGroup(final Type type) {
    return this.eventLoopGroupFactory.apply(this.name, type);
  }

  private static ThreadFactory createThreadFactory(final String name, final Type type) {
    return new VelocityNettyThreadFactory("Netty " + name + ' ' + type.toString() + " #%d");
  }

  public static TransportType bestType() {
    if (Boolean.getBoolean("velocity.disable-native-transport")) {
      return NIO;
    }

    if (Epoll.isAvailable()) {
      return EPOLL;
    } else {
      return NIO;
    }
  }

  public enum Type {
    BOSS("Boss"),
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
