package com.velocitypowered.proxy.network;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.velocitypowered.natives.util.Natives;
import com.velocitypowered.proxy.connection.MinecraftConnection;
import com.velocitypowered.proxy.connection.client.HandshakeSessionHandler;
import com.velocitypowered.proxy.protocol.ProtocolConstants;
import com.velocitypowered.proxy.protocol.StateRegistry;
import com.velocitypowered.proxy.protocol.netty.GS4QueryHandler;
import com.velocitypowered.proxy.protocol.netty.LegacyPingDecoder;
import com.velocitypowered.proxy.protocol.netty.LegacyPingEncoder;
import com.velocitypowered.proxy.protocol.netty.MinecraftDecoder;
import com.velocitypowered.proxy.protocol.netty.MinecraftEncoder;
import com.velocitypowered.proxy.protocol.netty.MinecraftVarintFrameDecoder;
import com.velocitypowered.proxy.protocol.netty.MinecraftVarintLengthEncoder;
import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollDatagramChannel;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.epoll.EpollSocketChannel;
import io.netty.channel.kqueue.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.DatagramChannel;
import io.netty.channel.socket.ServerSocketChannel;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.timeout.ReadTimeoutHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.InetSocketAddress;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import static com.velocitypowered.proxy.network.Connections.*;

public final class ConnectionManager {
    private static final Logger logger = LogManager.getLogger(ConnectionManager.class);

    private final Set<Channel> endpoints = new HashSet<>();
    private final TransportType transportType;
    private final EventLoopGroup bossGroup;
    private final EventLoopGroup workerGroup;

    public ConnectionManager() {
        this.transportType = TransportType.bestType();
        this.bossGroup = transportType.createEventLoopGroup(true);
        this.workerGroup = transportType.createEventLoopGroup(false);
        this.logChannelInformation();
    }

    private void logChannelInformation() {
        logger.info("Connections will use {} channels, {} compression, {} ciphers", transportType, Natives.compressor.getLoadedVariant(), Natives.cipher.getLoadedVariant());
    }

    public void bind(final InetSocketAddress address) {
        final ServerBootstrap bootstrap = new ServerBootstrap()
                .channel(this.transportType.serverSocketChannelClass)
                .group(this.bossGroup, this.workerGroup)
                .childHandler(new ChannelInitializer<Channel>() {
                    @Override
                    protected void initChannel(final Channel ch) {
                        ch.pipeline()
                                .addLast(READ_TIMEOUT, new ReadTimeoutHandler(CLIENT_READ_TIMEOUT_SECONDS, TimeUnit.SECONDS))
                                .addLast(LEGACY_PING_DECODER, new LegacyPingDecoder())
                                .addLast(FRAME_DECODER, new MinecraftVarintFrameDecoder())
                                .addLast(LEGACY_PING_ENCODER, LegacyPingEncoder.INSTANCE)
                                .addLast(FRAME_ENCODER, MinecraftVarintLengthEncoder.INSTANCE)
                                .addLast(MINECRAFT_DECODER, new MinecraftDecoder(ProtocolConstants.Direction.SERVERBOUND))
                                .addLast(MINECRAFT_ENCODER, new MinecraftEncoder(ProtocolConstants.Direction.CLIENTBOUND));

                        final MinecraftConnection connection = new MinecraftConnection(ch);
                        connection.setState(StateRegistry.HANDSHAKE);
                        connection.setSessionHandler(new HandshakeSessionHandler(connection));
                        ch.pipeline().addLast(Connections.HANDLER, connection);
                    }
                })
                .childOption(ChannelOption.TCP_NODELAY, true)
                .childOption(ChannelOption.IP_TOS, 0x18)
                .localAddress(address);
        bootstrap.bind()
                .addListener((ChannelFutureListener) future -> {
                    final Channel channel = future.channel();
                    if (future.isSuccess()) {
                        this.endpoints.add(channel);
                        logger.info("Listening on {}", channel.localAddress());
                    } else {
                        logger.error("Can't bind to {}", address, future.cause());
                    }
                });
    }

    public void queryBind(final String hostname, final int port) {
        Bootstrap bootstrap = new Bootstrap()
                .channel(transportType.datagramChannelClass)
                .group(this.workerGroup)
                .handler(new GS4QueryHandler())
                .localAddress(hostname, port);
        bootstrap.bind()
                .addListener((ChannelFutureListener) future -> {
                    final Channel channel = future.channel();
                    if (future.isSuccess()) {
                        this.endpoints.add(channel);
                        logger.info("Listening for GS4 query on {}", channel.localAddress());
                    } else {
                        logger.error("Can't bind to {}", bootstrap.config().localAddress(), future.cause());
                    }
                });
    }

    public Bootstrap createWorker() {
        return new Bootstrap()
                .channel(this.transportType.socketChannelClass)
                .group(this.workerGroup);
    }

    public void shutdown() {
        for (final Channel endpoint : this.endpoints) {
            try {
                logger.info("Closing endpoint {}", endpoint.localAddress());
                endpoint.close().sync();
            } catch (final InterruptedException e) {
                logger.info("Interrupted whilst closing endpoint", e);
            }
        }
    }

    private static ThreadFactory createThreadFactory(final String nameFormat) {
        return new ThreadFactoryBuilder()
                .setNameFormat(nameFormat)
                .setDaemon(true)
                .build();
    }

    private enum TransportType {
        NIO(NioServerSocketChannel.class, NioSocketChannel.class, NioDatagramChannel.class) {
            @Override
            public EventLoopGroup createEventLoopGroup(boolean boss) {
                String name = "Netty NIO " + (boss ? "Boss" : "Worker") + " #%d";
                return new NioEventLoopGroup(0, createThreadFactory(name));
            }
        },
        EPOLL(EpollServerSocketChannel.class, EpollSocketChannel.class, EpollDatagramChannel.class) {
            @Override
            public EventLoopGroup createEventLoopGroup(boolean boss) {
                String name = "Netty Epoll " + (boss ? "Boss" : "Worker") + " #%d";
                return new EpollEventLoopGroup(0, createThreadFactory(name));
            }
        },
        KQUEUE(KQueueServerSocketChannel.class, KQueueSocketChannel.class, KQueueDatagramChannel.class) {
            @Override
            public EventLoopGroup createEventLoopGroup(boolean boss) {
                String name = "Netty KQueue " + (boss ? "Boss" : "Worker") + " #%d";
                return new KQueueEventLoopGroup(0, createThreadFactory(name));
            }
        };

        private final Class<? extends ServerSocketChannel> serverSocketChannelClass;
        private final Class<? extends SocketChannel> socketChannelClass;
        private final Class<? extends DatagramChannel> datagramChannelClass;

        TransportType(Class<? extends ServerSocketChannel> serverSocketChannelClass, Class<? extends SocketChannel> socketChannelClass, Class<? extends DatagramChannel> datagramChannelClass) {
            this.serverSocketChannelClass = serverSocketChannelClass;
            this.socketChannelClass = socketChannelClass;
            this.datagramChannelClass = datagramChannelClass;
        }

        @Override
        public String toString() {
            return name().toLowerCase(Locale.US);
        }

        public abstract EventLoopGroup createEventLoopGroup(boolean boss);

        public static TransportType bestType() {
            if (Epoll.isAvailable()) {
                return EPOLL;
            } else if (KQueue.isAvailable()) {
                return KQUEUE;
            } else {
                return NIO;
            }
        }
    }
}
