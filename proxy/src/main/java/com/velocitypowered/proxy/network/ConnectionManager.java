package com.velocitypowered.proxy.network;

import com.velocitypowered.natives.util.Natives;
import com.velocitypowered.proxy.VelocityServer;
import com.velocitypowered.proxy.protocol.netty.GS4QueryHandler;
import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.WriteBufferWaterMark;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.checkerframework.checker.initialization.qual.UnderInitialization;

import java.net.InetSocketAddress;
import java.util.HashSet;
import java.util.Set;

public final class ConnectionManager {
    private static final WriteBufferWaterMark SERVER_WRITE_MARK = new WriteBufferWaterMark(1 << 16, 1 << 18);
    private static final Logger LOGGER = LogManager.getLogger(ConnectionManager.class);
    private final Set<Channel> endpoints = new HashSet<>();
    private final TransportType transportType;
    private final EventLoopGroup bossGroup;
    private final EventLoopGroup workerGroup;
    private final VelocityServer server;
    public final ServerChannelInitializerHolder serverChannelInitializer;

    public ConnectionManager(VelocityServer server) {
        this.server = server;
        this.transportType = TransportType.bestType();
        this.bossGroup = this.transportType.createEventLoopGroup(TransportType.Type.BOSS);
        this.workerGroup = this.transportType.createEventLoopGroup(TransportType.Type.WORKER);
        this.serverChannelInitializer = new ServerChannelInitializerHolder(new ServerChannelInitializer(this.server));
    }

    public void logChannelInformation() {
        LOGGER.info("Connections will use {} channels, {} compression, {} ciphers", this.transportType, Natives.compressor.getLoadedVariant(), Natives.cipher.getLoadedVariant());
    }

    public void bind(final InetSocketAddress address) {
        final ServerBootstrap bootstrap = new ServerBootstrap()
                .channel(this.transportType.serverSocketChannelClass)
                .group(this.bossGroup, this.workerGroup)
                .childOption(ChannelOption.WRITE_BUFFER_WATER_MARK, SERVER_WRITE_MARK)
                .childHandler(this.serverChannelInitializer.get())
                .childOption(ChannelOption.TCP_NODELAY, true)
                .childOption(ChannelOption.IP_TOS, 0x18)
                .localAddress(address);
        bootstrap.bind()
                .addListener((ChannelFutureListener) future -> {
                    final Channel channel = future.channel();
                    if (future.isSuccess()) {
                        this.endpoints.add(channel);
                        LOGGER.info("Listening on {}", channel.localAddress());
                    } else {
                        LOGGER.error("Can't bind to {}", address, future.cause());
                    }
                });
    }

    public void queryBind(final String hostname, final int port) {
        final Bootstrap bootstrap = new Bootstrap()
                .channel(this.transportType.datagramChannelClass)
                .group(this.workerGroup)
                .handler(new GS4QueryHandler(this.server))
                .localAddress(hostname, port);
        bootstrap.bind()
                .addListener((ChannelFutureListener) future -> {
                    final Channel channel = future.channel();
                    if (future.isSuccess()) {
                        this.endpoints.add(channel);
                        LOGGER.info("Listening for GS4 query on {}", channel.localAddress());
                    } else {
                        LOGGER.error("Can't bind to {}", bootstrap.config().localAddress(), future.cause());
                    }
                });
    }

    public Bootstrap createWorker() {
        return new Bootstrap()
                .channel(this.transportType.socketChannelClass)
                .group(this.workerGroup)
                .option(ChannelOption.TCP_NODELAY, true)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, this.server.getConfiguration().getConnectTimeout());
    }

    public void shutdown() {
        for (final Channel endpoint : this.endpoints) {
            try {
                LOGGER.info("Closing endpoint {}", endpoint.localAddress());
                endpoint.close().sync();
            } catch (final InterruptedException e) {
                LOGGER.info("Interrupted whilst closing endpoint", e);
            }
        }
    }

    public ServerChannelInitializerHolder getServerChannelInitializer() {
        return this.serverChannelInitializer;
    }
}
