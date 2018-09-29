package com.velocitypowered.proxy.connection.client;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.velocitypowered.api.event.connection.ConnectionHandshakeEvent;
import com.velocitypowered.api.event.proxy.ProxyPingEvent;
import com.velocitypowered.api.proxy.InboundConnection;
import com.velocitypowered.api.proxy.server.ServerPing;
import com.velocitypowered.proxy.VelocityServer;
import com.velocitypowered.proxy.config.PlayerInfoForwarding;
import com.velocitypowered.proxy.config.VelocityConfiguration;
import com.velocitypowered.proxy.connection.MinecraftConnection;
import com.velocitypowered.proxy.connection.MinecraftSessionHandler;
import com.velocitypowered.proxy.protocol.MinecraftPacket;
import com.velocitypowered.proxy.protocol.ProtocolConstants;
import com.velocitypowered.proxy.protocol.StateRegistry;
import com.velocitypowered.proxy.protocol.packet.*;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import net.kyori.text.TextComponent;
import net.kyori.text.TranslatableComponent;
import net.kyori.text.format.TextColor;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Optional;

public class HandshakeSessionHandler implements MinecraftSessionHandler {
    private final MinecraftConnection connection;
    private final VelocityServer server;

    public HandshakeSessionHandler(MinecraftConnection connection, VelocityServer server) {
        this.connection = Preconditions.checkNotNull(connection, "connection");
        this.server = Preconditions.checkNotNull(server, "server");
    }

    @Override
    public boolean handle(LegacyPing packet) {
        connection.setProtocolVersion(ProtocolConstants.LEGACY);
        VelocityConfiguration configuration = server.getConfiguration();
        ServerPing ping = new ServerPing(
                new ServerPing.Version(ProtocolConstants.MAXIMUM_GENERIC_VERSION, "Velocity " + ProtocolConstants.SUPPORTED_GENERIC_VERSION_STRING),
                new ServerPing.Players(server.getPlayerCount(), configuration.getShowMaxPlayers(), ImmutableList.of()),
                configuration.getMotdComponent(),
                null,
                null
        );
        ProxyPingEvent event = new ProxyPingEvent(new LegacyInboundConnection(connection), ping);
        server.getEventManager().fire(event)
                .thenRunAsync(() -> {
                    // The disconnect packet is the same as the server response one.
                    connection.closeWith(LegacyDisconnect.fromPingResponse(LegacyPingResponse.from(event.getPing())));
                }, connection.eventLoop());
        return true;
    }

    @Override
    public boolean handle(LegacyHandshake packet) {
        connection.closeWith(LegacyDisconnect.from(TextComponent.of("Your client is old, please upgrade!", TextColor.RED)));
        return true;
    }

    @Override
    public boolean handle(Handshake handshake) {
        InitialInboundConnection ic = new InitialInboundConnection(connection, handshake);
        switch (handshake.getNextStatus()) {
            case StateRegistry.STATUS_ID:
                connection.setState(StateRegistry.STATUS);
                connection.setProtocolVersion(handshake.getProtocolVersion());
                connection.setSessionHandler(new StatusSessionHandler(server, connection, ic));
                return true;
            case StateRegistry.LOGIN_ID:
                connection.setState(StateRegistry.LOGIN);
                connection.setProtocolVersion(handshake.getProtocolVersion());

                if (!ProtocolConstants.isSupported(handshake.getProtocolVersion())) {
                    connection.closeWith(Disconnect.create(TranslatableComponent.of("multiplayer.disconnect.outdated_client")));
                    return true;
                }

                InetAddress address = ((InetSocketAddress) connection.getChannel().remoteAddress()).getAddress();
                if (!server.getIpAttemptLimiter().attempt(address)) {
                    connection.closeWith(Disconnect.create(TextComponent.of("You are logging in too fast, try again later.")));
                    return true;
                }

                // Determine if we're using Forge (1.8 to 1.12, may not be the case in 1.13) and store that in the connection
                boolean isForge = handshake.getServerAddress().endsWith("\0FML\0");
                connection.setLegacyForge(isForge);

                // Make sure legacy forwarding is not in use on this connection. Make sure that we do _not_ reject Forge
                if (handshake.getServerAddress().contains("\0") && !isForge) {
                    connection.closeWith(Disconnect.create(TextComponent.of("Running Velocity behind Velocity is unsupported.")));
                    return true;
                }

                // If the proxy is configured for modern forwarding, we must deny connections from 1.12.2 and lower,
                // otherwise IP information will never get forwarded.
                if (server.getConfiguration().getPlayerInfoForwardingMode() == PlayerInfoForwarding.MODERN && handshake.getProtocolVersion() <
                        ProtocolConstants.MINECRAFT_1_13) {
                    connection.closeWith(Disconnect.create(TextComponent.of("This server is only compatible with 1.13 and above.")));
                    return true;
                }

                server.getEventManager().fireAndForget(new ConnectionHandshakeEvent(ic));
                connection.setSessionHandler(new LoginSessionHandler(server, connection, ic));
                return true;
            default:
                throw new IllegalArgumentException("Invalid state " + handshake.getNextStatus());
        }
    }

    @Override
    public void handleGeneric(MinecraftPacket packet) {

    }

    @Override
    public void handleUnknown(ByteBuf buf) {
        // what even is going on?
        connection.close();
    }

    private static class LegacyInboundConnection implements InboundConnection {
        private final MinecraftConnection connection;

        private LegacyInboundConnection(MinecraftConnection connection) {
            this.connection = connection;
        }

        @Override
        public InetSocketAddress getRemoteAddress() {
            return (InetSocketAddress) connection.getChannel().remoteAddress();
        }

        @Override
        public Optional<InetSocketAddress> getVirtualHost() {
            return Optional.empty();
        }

        @Override
        public boolean isActive() {
            return !connection.isClosed();
        }

        @Override
        public int getProtocolVersion() {
            return 0;
        }
    }
}
