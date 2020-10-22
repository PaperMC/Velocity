package com.velocitypowered.api.event.player;

import com.google.common.base.Preconditions;
import com.velocitypowered.api.proxy.connection.Player;

/**
 * This event is fired once the player has been fully initialized and is about to connect to their
 * first server.
 */
public final class PostLoginEvent {

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
