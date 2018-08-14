package com.velocitypowered.proxy.connection.client;

import com.google.common.base.Preconditions;
import com.velocitypowered.api.event.connection.ConnectionHandshakeEvent;
import com.velocitypowered.api.event.proxy.ProxyPingEvent;
import com.velocitypowered.api.proxy.InboundConnection;
import com.velocitypowered.proxy.VelocityServer;
import com.velocitypowered.proxy.config.VelocityConfiguration;
import com.velocitypowered.proxy.connection.MinecraftConnection;
import com.velocitypowered.proxy.connection.MinecraftSessionHandler;
import com.velocitypowered.api.server.ServerPing;
import com.velocitypowered.proxy.protocol.MinecraftPacket;
import com.velocitypowered.proxy.protocol.ProtocolConstants;
import com.velocitypowered.proxy.protocol.StateRegistry;
import com.velocitypowered.proxy.protocol.packet.*;
import net.kyori.text.TextComponent;
import net.kyori.text.TranslatableComponent;
import net.kyori.text.format.TextColor;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Optional;

public class HandshakeSessionHandler implements MinecraftSessionHandler {
    private final MinecraftConnection connection;

    public HandshakeSessionHandler(MinecraftConnection connection) {
        this.connection = Preconditions.checkNotNull(connection, "connection");
    }

    @Override
    public void handle(MinecraftPacket packet) {
        if (packet instanceof LegacyPing || packet instanceof LegacyHandshake) {
            connection.setProtocolVersion(ProtocolConstants.LEGACY);
            handleLegacy(packet);
            return;
        }

        if (!(packet instanceof Handshake)) {
            throw new IllegalArgumentException("Did not expect packet " + packet.getClass().getName());
        }

        InitialInboundConnection ic = new InitialInboundConnection(connection, (Handshake) packet);

        Handshake handshake = (Handshake) packet;
        switch (handshake.getNextStatus()) {
            case StateRegistry.STATUS_ID:
                connection.setState(StateRegistry.STATUS);
                connection.setProtocolVersion(handshake.getProtocolVersion());
                connection.setSessionHandler(new StatusSessionHandler(connection, ic));
                break;
            case StateRegistry.LOGIN_ID:
                connection.setState(StateRegistry.LOGIN);
                connection.setProtocolVersion(handshake.getProtocolVersion());
                if (!ProtocolConstants.isSupported(handshake.getProtocolVersion())) {
                    connection.closeWith(Disconnect.create(TranslatableComponent.of("multiplayer.disconnect.outdated_client")));
                    return;
                } else {
                    InetAddress address = ((InetSocketAddress) connection.getChannel().remoteAddress()).getAddress();
                    if (!VelocityServer.getServer().getIpAttemptLimiter().attempt(address)) {
                        connection.closeWith(Disconnect.create(TextComponent.of("You are logging in too fast, try again later.")));
                        return;
                    }
                    VelocityServer.getServer().getEventManager().fireAndForget(new ConnectionHandshakeEvent(ic));
                    connection.setSessionHandler(new LoginSessionHandler(connection, ic));
                }
                break;
            default:
                throw new IllegalArgumentException("Invalid state " + handshake.getNextStatus());
        }
    }

    private void handleLegacy(MinecraftPacket packet) {
        if (packet instanceof LegacyPing) {
            VelocityConfiguration configuration = VelocityServer.getServer().getConfiguration();
            ServerPing ping = new ServerPing(
                    new ServerPing.Version(ProtocolConstants.MAXIMUM_GENERIC_VERSION, "Velocity " + ProtocolConstants.SUPPORTED_GENERIC_VERSION_STRING),
                    new ServerPing.Players(VelocityServer.getServer().getPlayerCount(), configuration.getShowMaxPlayers(), sample),
                    configuration.getMotdComponent(),
                    null
            );
            ProxyPingEvent event = new ProxyPingEvent(new LegacyInboundConnection(connection), ping);
            VelocityServer.getServer().getEventManager().fire(event)
                    .thenRunAsync(() -> {
                        // The disconnect packet is the same as the server response one.
                        connection.closeWith(LegacyDisconnect.fromPingResponse(LegacyPingResponse.from(event.getPing())));
                    }, connection.getChannel().eventLoop());
        } else if (packet instanceof LegacyHandshake) {
            connection.closeWith(LegacyDisconnect.from(TextComponent.of("Your client is old, please upgrade!", TextColor.RED)));
        }
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
