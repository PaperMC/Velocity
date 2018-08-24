package com.velocitypowered.api.event.connection;

import com.google.common.base.Preconditions;
import com.velocitypowered.api.event.ResultedEvent;
import com.velocitypowered.api.proxy.InboundConnection;
import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * This event is fired when a player has initiated a connection with the proxy but before the proxy authenticates the
 * player with Mojang or before the player's proxy connection is fully established (for offline mode).
 */
public class PreLoginEvent implements ResultedEvent<ResultedEvent.ComponentResult> {
    private final InboundConnection connection;
    private final String username;
    private boolean onlineMode;
    private ComponentResult result;

    public PreLoginEvent(InboundConnection connection, String username, boolean onlineMode) {
        this.connection = Preconditions.checkNotNull(connection, "connection");
        this.username = Preconditions.checkNotNull(username, "username");
        this.onlineMode = onlineMode;
        this.result = ComponentResult.allowed();
    }

    public InboundConnection getConnection() {
        return connection;
    }

    public String getUsername() {
        return username;
    }

    public boolean isOnlineMode() {
        return onlineMode;
    }
    
    public void setOnlineMode(boolean onlineMode) {
        this.onlineMode = onlineMode;
    }
    
    @Override
    public ComponentResult getResult() {
        return result;
    }

    @Override
    public void setResult(@NonNull ComponentResult result) {
        this.result = Preconditions.checkNotNull(result, "result");
    }

    @Override
    public String toString() {
        return "PreLoginEvent{" +
                "connection=" + connection +
                ", username='" + username + '\'' +
                ", onlineMode=" + onlineMode +
                ", result=" + result +
                '}';
    }
}
