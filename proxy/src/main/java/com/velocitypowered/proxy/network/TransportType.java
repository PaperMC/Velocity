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
import io.netty5.channel.ChannelFactory;
import io.netty5.channel.EventLoopGroup;
import io.netty5.channel.MultithreadEventLoopGroup;
import io.netty5.channel.epoll.Epoll;
import io.netty5.channel.epoll.EpollDatagramChannel;
import io.netty5.channel.epoll.EpollHandler;
import io.netty5.channel.epoll.EpollServerSocketChannel;
import io.netty5.channel.epoll.EpollSocketChannel;
import io.netty5.channel.nio.NioHandler;
import io.netty5.channel.socket.DatagramChannel;
import io.netty5.channel.socket.ServerSocketChannel;
import io.netty5.channel.socket.SocketChannel;
import io.netty5.channel.socket.nio.NioDatagramChannel;
import io.netty5.channel.socket.nio.NioServerSocketChannel;
import io.netty5.channel.socket.nio.NioSocketChannel;
import java.util.concurrent.ThreadFactory;
import java.util.function.BiFunction;

enum TransportType {
  NIO("NIO", NioServerSocketChannel.class,
      NioSocketChannel::new,
      NioDatagramChannel::new,
      (name, type) -> new MultithreadEventLoopGroup(0,
          createThreadFactory(name, type), NioHandler.newFactory())),
  EPOLL("epoll", EpollServerSocketChannel.class,
      EpollSocketChannel::new,
      EpollDatagramChannel::new,
      (name, type) -> new MultithreadEventLoopGroup(0,
          createThreadFactory(name, type), EpollHandler.newFactory()));

  final String name;
  final Class<? extends ServerSocketChannel> serverSocketChannelClass;
  final ChannelFactory<? extends SocketChannel> socketChannelFactory;
  final ChannelFactory<? extends DatagramChannel> datagramChannelFactory;
  final BiFunction<String, Type, EventLoopGroup> eventLoopGroupFactory;

  TransportType(final String name,
      final Class<? extends ServerSocketChannel> serverSocketChannelClass,
      final ChannelFactory<? extends SocketChannel> socketChannelFactory,
      final ChannelFactory<? extends DatagramChannel> datagramChannelFactory,
      final BiFunction<String, Type, EventLoopGroup> eventLoopGroupFactory) {
    this.name = name;
    this.serverSocketChannelClass = serverSocketChannelClass;
    this.socketChannelFactory = socketChannelFactory;
    this.datagramChannelFactory = datagramChannelFactory;
    this.eventLoopGroupFactory = eventLoopGroupFactory;
  }

  @Override
  public String toString() {
    return this.name;
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
