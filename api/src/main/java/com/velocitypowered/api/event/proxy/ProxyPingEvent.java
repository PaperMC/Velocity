package com.velocitypowered.api.event.proxy;

import com.google.common.base.Preconditions;
import com.velocitypowered.api.proxy.InboundConnection;
import com.velocitypowered.api.server.ServerPing;

import javax.annotation.Nonnull;

public class ProxyPingEvent {
    private final InboundConnection connection;
    private ServerPing ping;

    public ProxyPingEvent(InboundConnection connection, ServerPing ping) {
        this.connection = Preconditions.checkNotNull(connection, "connection");
        this.ping = Preconditions.checkNotNull(ping, "ping");
    }

    public InboundConnection getConnection() {
        return connection;
    }

    public ServerPing getPing() {
        return ping;
    }

    public void setPing(@Nonnull ServerPing ping) {
        this.ping = Preconditions.checkNotNull(ping, "ping");
    }

    @Override
    public String toString() {
        return "ProxyPingEvent{" +
                "connection=" + connection +
                ", ping=" + ping +
                '}';
    }
}
