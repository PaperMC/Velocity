package com.velocitypowered.api.event.connection;

import com.google.common.base.Preconditions;
import com.velocitypowered.api.proxy.Player;

/**
 * This event is fired once the player has been successfully authenticated and
 * fully initialized and player will be connected to server after this event
 */
public class PostLoginEvent {

    private final Player player;

    public PostLoginEvent(Player player) {
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
