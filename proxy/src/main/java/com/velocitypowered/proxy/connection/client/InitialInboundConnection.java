package com.velocitypowered.proxy.connection.client;

import com.velocitypowered.api.proxy.InboundConnection;
import com.velocitypowered.proxy.connection.MinecraftConnection;
import com.velocitypowered.proxy.protocol.packet.Handshake;

import java.net.InetSocketAddress;
import java.util.Optional;

class InitialInboundConnection implements InboundConnection {
    private final MinecraftConnection connection;
    private final Handshake handshake;

    InitialInboundConnection(MinecraftConnection connection, Handshake handshake) {
        this.connection = connection;
        this.handshake = handshake;
    }

    @Override
    public InetSocketAddress getRemoteAddress() {
        return (InetSocketAddress) connection.getChannel().remoteAddress();
    }

    @Override
    public Optional<InetSocketAddress> getVirtualHost() {
        return Optional.of(InetSocketAddress.createUnresolved(handshake.getServerAddress(), handshake.getPort()));
    }

    @Override
    public boolean isActive() {
        return connection.getChannel().isActive();
    }

    @Override
    public int getProtocolVersion() {
        return connection.getProtocolVersion();
    }
}
