package com.velocitypowered.proxy.server.ping;

import com.google.common.base.Preconditions;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.proxy.server.ServerPing;
import com.velocitypowered.proxy.VelocityServer;
import com.velocitypowered.proxy.connection.MinecraftConnection;
import com.velocitypowered.proxy.connection.MinecraftSessionHandler;
import com.velocitypowered.proxy.protocol.MinecraftPacket;
import com.velocitypowered.proxy.protocol.ProtocolConstants;
import com.velocitypowered.proxy.protocol.StateRegistry;
import com.velocitypowered.proxy.protocol.packet.Handshake;
import com.velocitypowered.proxy.protocol.packet.StatusRequest;
import com.velocitypowered.proxy.protocol.packet.StatusResponse;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

public class PingSessionHandler implements MinecraftSessionHandler {
    private final CompletableFuture<ServerPing> result;
    private final RegisteredServer server;
    private final MinecraftConnection connection;
    private boolean completed = false;

    public PingSessionHandler(CompletableFuture<ServerPing> result, RegisteredServer server, MinecraftConnection connection) {
        this.result = result;
        this.server = server;
        this.connection = connection;
    }

    @Override
    public void activated() {
        Handshake handshake = new Handshake();
        handshake.setNextStatus(StateRegistry.STATUS_ID);
        handshake.setServerAddress(server.getServerInfo().getAddress().getHostString());
        handshake.setPort(server.getServerInfo().getAddress().getPort());
        handshake.setProtocolVersion(ProtocolConstants.MINIMUM_GENERIC_VERSION);
        connection.write(handshake);

        connection.setState(StateRegistry.STATUS);
        connection.write(new StatusRequest());
    }

    @Override
    public void handle(MinecraftPacket packet) {
        Preconditions.checkState(packet instanceof StatusResponse, "Did not get status response back from connection");

        // All good!
        completed = true;
        connection.close();

        ServerPing ping = VelocityServer.GSON.fromJson(((StatusResponse) packet).getStatus(), ServerPing.class);
        result.complete(ping);
    }

    @Override
    public void disconnected() {
        if (!completed) {
            result.completeExceptionally(new IOException("Unexpectedly disconnected from remote server"));
        }
    }

    @Override
    public void exception(Throwable throwable) {
        completed = true;
        result.completeExceptionally(throwable);
    }
}
