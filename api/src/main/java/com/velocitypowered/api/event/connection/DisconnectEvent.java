package com.velocitypowered.api.event.connection;

import com.google.common.base.Preconditions;
import com.velocitypowered.api.proxy.Player;
import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * This event is fired when a player disconnects from the proxy.
 */
public class DisconnectEvent {
    private @NonNull final Player player;

    public DisconnectEvent(@NonNull Player player) {
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
