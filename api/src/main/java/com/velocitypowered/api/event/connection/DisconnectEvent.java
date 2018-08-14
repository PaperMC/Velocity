package com.velocitypowered.api.event.connection;

import com.velocitypowered.api.proxy.Player;

/**
 * This event is fired when a player disconnects from the proxy.
 */
public class DisconnectEvent {
    private final Player player;

    public DisconnectEvent(Player player) {
        this.player = player;
    }

    public Player getPlayer() {
        return player;
    }

    @Override
    public String toString() {
        return "DisconnectEvent{" +
                "player=" + player +
                '}';
    }
}
