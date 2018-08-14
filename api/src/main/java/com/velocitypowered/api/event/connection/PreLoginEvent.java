package com.velocitypowered.api.event.connection;

import com.google.common.base.Preconditions;
import com.velocitypowered.api.event.ResultedEvent;
import com.velocitypowered.api.proxy.InboundConnection;

import javax.annotation.Nonnull;

/**
 * This event is fired when a player has initiated a connection with the proxy but before the proxy authenticates the
 * player with Mojang or before the player's proxy connection is fully established (for offline mode).
 */
public class PreLoginEvent implements ResultedEvent<ResultedEvent.ComponentResult> {
    private final InboundConnection connection;
    private final String username;
    private ComponentResult result;

    public PreLoginEvent(InboundConnection connection, String username) {
        this.connection = connection;
        this.username = username;
        this.result = ComponentResult.allowed();
    }

    public InboundConnection getConnection() {
        return connection;
    }

    public String getUsername() {
        return username;
    }

    @Override
    public ComponentResult getResult() {
        return result;
    }

    @Override
    public void setResult(@Nonnull ComponentResult result) {
        this.result = Preconditions.checkNotNull(result, "result");
    }

    @Override
    public String toString() {
        return "PreLoginEvent{" +
                "connection=" + connection +
                ", username='" + username + '\'' +
                ", result=" + result +
                '}';
    }
}
