package com.velocitypowered.api.event.connection;

import com.google.common.base.Preconditions;
import com.velocitypowered.api.proxy.Player;

/**
 * This event is fired when a player disconnects from the proxy. Operations on the provided player, aside from basic
 * data retrieval operations, may behave in undefined ways.
 */
public final class DisconnectEvent {
    private final Player player;

    public DisconnectEvent(Player player) {
        this.player = Preconditions.checkNotNull(player, "player");
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
