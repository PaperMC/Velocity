package com.velocitypowered.proxy.connection;

import com.velocitypowered.proxy.connection.backend.VelocityServerConnection;
import java.util.Optional;

/**
 * This event is used to allow add custom packet handlers to
 * MinecraftConnection. Use this event only if you know what you do. This event
 * can fired sync. Use this event only for adding custom handlers
 */
@Deprecated
public class ConnectionInitializeEvent {

    private final MinecraftConnection minecraftConnection;
    private final VelocityServerConnection server;

    public ConnectionInitializeEvent(MinecraftConnection connection, VelocityServerConnection server) {
        this.minecraftConnection = connection;
        this.server = server;
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
        return Optional.ofNullable(server);
    }

    /**
     * @return true if connection is a server connection(Between proxy and
     * server), false if connection is a client connection(Between client and
     * proxy)
     */
    public boolean isBackendConnection() {
        return server != null;
    }

}
