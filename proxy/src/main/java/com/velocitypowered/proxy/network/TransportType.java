package com.velocitypowered.proxy.network;

import com.velocitypowered.proxy.util.concurrent.VelocityNettyThreadFactory;
import io.netty.channel.ChannelFactory;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.ReflectiveChannelFactory;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollDatagramChannel;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.epoll.EpollSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.DatagramChannel;
import io.netty.channel.socket.ServerSocketChannel;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import java.util.concurrent.ThreadFactory;
import java.util.function.BiFunction;

enum TransportType {
  NIO("NIO", NioServerSocketChannel::new, NioServerSocketChannel.class,
      NioSocketChannel::new, NioSocketChannel.class,
      NioDatagramChannel::new, NioDatagramChannel.class,
      (name, type) -> new NioEventLoopGroup(0, createThreadFactory(name, type))),
  EPOLL("epoll", EpollServerSocketChannel::new, EpollServerSocketChannel.class,
      EpollSocketChannel::new, EpollSocketChannel.class,
      EpollDatagramChannel::new, EpollDatagramChannel.class,
      (name, type) -> new EpollEventLoopGroup(0, createThreadFactory(name, type)));

  final String name;
  @Deprecated
  final Class<? extends ServerSocketChannel> serverSocketChannelClass;
  @Deprecated
  final Class<? extends SocketChannel> socketChannelClass;
  @Deprecated
  final Class<? extends DatagramChannel> datagramChannelClass;
  final ChannelFactory<? extends ServerSocketChannel> serverSocketChannelFactory;
  final ChannelFactory<? extends SocketChannel> socketChannelFactory;
  final ChannelFactory<? extends DatagramChannel> datagramChannelFactory;
  final BiFunction<String, Type, EventLoopGroup> eventLoopGroupFactory;

  TransportType(final String name,
                final ChannelFactory<? extends ServerSocketChannel> serverSocketChannelFactory,
                final Class<? extends ServerSocketChannel> serverSocketChannelClass,
                final ChannelFactory<? extends SocketChannel> socketChannelFactory,
                final Class<? extends SocketChannel> socketChannelClass,
                final ChannelFactory<? extends DatagramChannel> datagramChannelFactory,
                final Class<? extends DatagramChannel> datagramChannelClass,
                final BiFunction<String, Type, EventLoopGroup> eventLoopGroupFactory) {
    this.name = name;
    this.serverSocketChannelClass = serverSocketChannelClass;
    this.socketChannelClass = socketChannelClass;
    this.datagramChannelClass = datagramChannelClass;
    this.serverSocketChannelFactory = serverSocketChannelFactory;
    this.socketChannelFactory = socketChannelFactory;
    this.datagramChannelFactory = datagramChannelFactory;
    this.eventLoopGroupFactory = eventLoopGroupFactory;
  }

  @Deprecated
  TransportType(final String name,
      final Class<? extends ServerSocketChannel> serverSocketChannelClass,
      final Class<? extends SocketChannel> socketChannelClass,
      final Class<? extends DatagramChannel> datagramChannelClass,
      final BiFunction<String, Type, EventLoopGroup> eventLoopGroupFactory) {
    this(name, new ReflectiveChannelFactory<>(serverSocketChannelClass), serverSocketChannelClass,
            new ReflectiveChannelFactory<>(socketChannelClass), socketChannelClass,
            new ReflectiveChannelFactory<>(datagramChannelClass), datagramChannelClass,
            eventLoopGroupFactory);
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
