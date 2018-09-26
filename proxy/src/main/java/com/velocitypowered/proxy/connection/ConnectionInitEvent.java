package com.velocitypowered.proxy.connection;

import com.velocitypowered.api.proxy.server.RegisteredServer;

/**
 * This event is used to allow add custom packet handlers to
 * MinecraftConnection. Use this event only if you know what you do. This event
 * can fired sync. Use this event only for adding custom handlers
 */
@Deprecated
public class ConnectionInitEvent {

    private final MinecraftConnection minecraftConnection;
    private final RegisteredServer server;

    public ConnectionInitEvent(MinecraftConnection connection, RegisteredServer server) {
        this.minecraftConnection = connection;
        this.server = server;
    }

    public MinecraftConnection getMinecraftConnection() {
        return minecraftConnection;
    }

    public RegisteredServer getServer() {
        return server;
    }

    /**
     * @return true if connection is a server connection(Between proxy and
     * server), false if connection is a client connection(Between client and
     * proxy)
     */
    public boolean isBackendConnection() {
        return getServer() != null;
    }

}
