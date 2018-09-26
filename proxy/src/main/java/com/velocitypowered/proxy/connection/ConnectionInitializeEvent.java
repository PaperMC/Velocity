package com.velocitypowered.proxy.connection;

import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.proxy.connection.backend.VelocityServerConnection;
import com.velocitypowered.proxy.server.VelocityRegisteredServer;
import java.util.Optional;

/**
 * This event is used to allow add custom packet handlers to
 * MinecraftConnection. Use this event only if you know what you do. This event
 * can fired sync. Use this event only for adding custom handlers
 */
@Deprecated
public class ConnectionInitializeEvent {

    private final MinecraftConnection minecraftConnection;
    private final VelocityServerConnection serverConnection;
    private final VelocityRegisteredServer registeredServer;

    public ConnectionInitializeEvent(MinecraftConnection connection, VelocityServerConnection serverConnection, VelocityRegisteredServer registeredServer) {
        this.minecraftConnection = connection;
        this.serverConnection = serverConnection;
        this.registeredServer = registeredServer;
    }

    public MinecraftConnection getMinecraftConnection() {
        return minecraftConnection;
    }

    /**
     *
     * @return Optional of VelocityServerConnection that can be empty if
     * connection is not backend ({@link ConnectionInitializeEvent#isBackendConnection())
     */
    public Optional<VelocityServerConnection> getServerConnection() {
        return Optional.ofNullable(serverConnection);
    }

    /**
     *
     * @return Optional of VelocityRegisteredServer if this connection created
     * by {@link RegisteredServer#ping()) or empty if not
     */
    public Optional<VelocityRegisteredServer> getRegisteredServer() {
        return Optional.ofNullable(registeredServer);
    }

    /**
     * @return true if connection is a server connection(Between proxy and
     * server), false if connection is a client connection(Between client and
     * proxy)
     */
    public boolean isBackendConnection() {
        return serverConnection != null;
    }

    /**
     *
     * @return true if connection is created by {@link RegisteredServer#ping())
     */
    public boolean isPingConnection() {
        return registeredServer != null;
    }

}
