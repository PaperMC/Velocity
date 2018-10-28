package com.velocitypowered.api.event.connection;

import com.google.common.base.Preconditions;
import com.velocitypowered.api.proxy.InboundConnection;

/**
 * This event is fired when a handshake is established between a client and Velocity.
 */
public final class ConnectionHandshakeEvent {
    private final InboundConnection connection;

    public ConnectionHandshakeEvent(InboundConnection connection) {
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
