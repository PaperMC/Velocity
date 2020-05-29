package com.velocitypowered.api.event.connection;

import com.velocitypowered.api.proxy.InboundConnection;

/**
 * This event is fired when login process has failed for some reason
 */
public final class LoginFailEvent {

    private final InboundConnection connection;
    private final String username;
    private final Reason reason;

    public LoginFailEvent(InboundConnection connection, String username, Reason reason) {
        this.connection = connection;
        this.username = username;
        this.reason = reason;
    }

    public InboundConnection getConnection() {
        return connection;
    }

    public String getUsername() {
        return username;
    }

    public Reason getReason() {
        return reason;
    }

    public enum Reason {

        ONLINE_MODE_ONLY,
        ALREADY_CONNECTED,
        NO_AVAILABLE_SERVERS,
        PLAYER_INITIATED,
        PLUGIN,
        UNDEFINED,

    }
}
