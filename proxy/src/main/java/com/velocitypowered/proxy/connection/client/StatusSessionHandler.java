package com.velocitypowered.proxy.connection.client;

import com.google.common.collect.ImmutableList;
import com.velocitypowered.api.event.proxy.ProxyPingEvent;
import com.velocitypowered.api.proxy.InboundConnection;
import com.velocitypowered.api.proxy.server.ServerPing;
import com.velocitypowered.proxy.VelocityServer;
import com.velocitypowered.proxy.config.VelocityConfiguration;
import com.velocitypowered.proxy.connection.MinecraftConnection;
import com.velocitypowered.proxy.connection.MinecraftSessionHandler;
import com.velocitypowered.proxy.protocol.ProtocolConstants;
import com.velocitypowered.proxy.protocol.packet.StatusPing;
import com.velocitypowered.proxy.protocol.packet.StatusRequest;
import com.velocitypowered.proxy.protocol.packet.StatusResponse;
import io.netty.buffer.ByteBuf;

public class StatusSessionHandler implements MinecraftSessionHandler {
    private final VelocityServer server;
    private final MinecraftConnection connection;
    private final InboundConnection inboundWrapper;

    StatusSessionHandler(VelocityServer server, MinecraftConnection connection, InboundConnection inboundWrapper) {
        this.server = server;
        this.connection = connection;
        this.inboundWrapper = inboundWrapper;
    }

    @Override
    public boolean handle(StatusPing packet) {
        connection.closeWith(packet);
        return true;
    }

    @Override
    public boolean handle(StatusRequest packet) {
        VelocityConfiguration configuration = server.getConfiguration();

        int shownVersion = ProtocolConstants.isSupported(connection.getProtocolVersion()) ? connection.getProtocolVersion() :
                ProtocolConstants.MAXIMUM_GENERIC_VERSION;
        ServerPing initialPing = new ServerPing(
                new ServerPing.Version(shownVersion, "Velocity " + ProtocolConstants.SUPPORTED_GENERIC_VERSION_STRING),
                new ServerPing.Players(server.getPlayerCount(), configuration.getShowMaxPlayers(), ImmutableList.of()),
                configuration.getMotdComponent(),
                configuration.getFavicon().orElse(null),
                configuration.isAnnounceForge() ? ServerPing.ModInfo.DEFAULT : null
        );

        ProxyPingEvent event = new ProxyPingEvent(inboundWrapper, initialPing);
        server.getEventManager().fire(event)
                .thenRunAsync(() -> {
                    StatusResponse response = new StatusResponse();
                    response.setStatus(VelocityServer.GSON.toJson(event.getPing()));
                    connection.write(response);
                }, connection.eventLoop());
        return true;
    }

    @Override
    public void handleUnknown(ByteBuf buf) {
        // what even is going on?
        connection.close();
    }
}
