package io.minimum.minecraft.velocity.proxy;

import io.minimum.minecraft.velocity.proxy.server.ServerConnection;

import java.util.UUID;

public class ConnectedPlayer {
    private final String username;
    private final UUID uniqueId;
    private final InboundMinecraftConnection connection;
    private ServerConnection connectedServer;

    public ConnectedPlayer(String username, UUID uniqueId, InboundMinecraftConnection connection) {
        this.username = username;
        this.uniqueId = uniqueId;
        this.connection = connection;
    }

    public String getUsername() {
        return username;
    }

    public UUID getUniqueId() {
        return uniqueId;
    }

    public InboundMinecraftConnection getConnection() {
        return connection;
    }

    public ServerConnection getConnectedServer() {
        return connectedServer;
    }
}
