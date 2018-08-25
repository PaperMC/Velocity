package com.velocitypowered.proxy.connection.client;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.velocitypowered.api.event.proxy.ProxyPingEvent;
import com.velocitypowered.api.proxy.InboundConnection;
import com.velocitypowered.proxy.VelocityServer;
import com.velocitypowered.proxy.config.VelocityConfiguration;
import com.velocitypowered.proxy.protocol.MinecraftPacket;
import com.velocitypowered.proxy.protocol.ProtocolConstants;
import com.velocitypowered.proxy.protocol.packet.StatusPing;
import com.velocitypowered.proxy.protocol.packet.StatusRequest;
import com.velocitypowered.proxy.protocol.packet.StatusResponse;
import com.velocitypowered.proxy.connection.MinecraftConnection;
import com.velocitypowered.api.proxy.server.ServerPing;
import com.velocitypowered.proxy.connection.MinecraftSessionHandler;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;

public class StatusSessionHandler implements MinecraftSessionHandler {
    private final MinecraftConnection connection;
    private final InboundConnection inboundWrapper;

    public StatusSessionHandler(MinecraftConnection connection, InboundConnection inboundWrapper) {
        this.connection = connection;
        this.inboundWrapper = inboundWrapper;
    }

    @Override
    public void handle(MinecraftPacket packet) {
        Preconditions.checkArgument(packet instanceof StatusPing || packet instanceof StatusRequest,
                "Unrecognized packet type " + packet.getClass().getName());

        if (packet instanceof StatusPing) {
            // Just send back the client's packet, no processing to do here.
            connection.closeWith(packet);
            return;
        }

        VelocityConfiguration configuration = VelocityServer.getServer().getConfiguration();

        // Status request
        int shownVersion = ProtocolConstants.isSupported(connection.getProtocolVersion()) ? connection.getProtocolVersion() :
                ProtocolConstants.MAXIMUM_GENERIC_VERSION;
        ServerPing initialPing = new ServerPing(
                new ServerPing.Version(shownVersion, "Velocity " + ProtocolConstants.SUPPORTED_GENERIC_VERSION_STRING),
                new ServerPing.Players(VelocityServer.getServer().getPlayerCount(), configuration.getShowMaxPlayers(), ImmutableList.of()),
                configuration.getMotdComponent(),
                configuration.getFavicon()
        );

        ProxyPingEvent event = new ProxyPingEvent(inboundWrapper, initialPing);
        VelocityServer.getServer().getEventManager().fire(event)
                .thenRunAsync(() -> {
                    StatusResponse response = new StatusResponse();
                    response.setStatus(VelocityServer.GSON.toJson(event.getPing()));
                    connection.write(response);
                }, connection.getChannel().eventLoop());
    }

    @Override
    public void handleUnknown(ByteBuf buf) {
        throw new IllegalStateException("Unknown data " + ByteBufUtil.hexDump(buf));
    }
}
