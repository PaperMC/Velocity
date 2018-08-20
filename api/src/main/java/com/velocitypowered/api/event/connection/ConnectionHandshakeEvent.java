package com.velocitypowered.api.event.connection;

import com.google.common.base.Preconditions;
import com.velocitypowered.api.proxy.InboundConnection;
import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * This event is fired when a handshake is established between a client and Velocity.
 */
public class ConnectionHandshakeEvent {
    private final @NonNull InboundConnection connection;

    public ConnectionHandshakeEvent(@NonNull InboundConnection connection) {
        this.connection = Preconditions.checkNotNull(connection, "connection");
    }

    public InboundConnection getConnection() {
        return connection;
    }

    @Override
    public String toString() {
        return "ConnectionHandshakeEvent{" +
                "connection=" + connection +
                '}';
    }
}
