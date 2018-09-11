package com.velocitypowered.api.event.connection;

import com.google.common.base.Preconditions;
import com.velocitypowered.api.proxy.Player;
import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * This event is fired once the player has been successfully authenticated and
 * fully initialized and player will be connected to server after this event
 */
public class PostLoginEvent {

    private final Player player;

    public PostLoginEvent(@NonNull Player player) {
        this.player = Preconditions.checkNotNull(player, "player");
    }

    public Player getPlayer() {
        return player;
    }

    @Override
    public String toString() {
        return "PostLoginEvent{"
                + "player=" + player
                + '}';
    }
}
