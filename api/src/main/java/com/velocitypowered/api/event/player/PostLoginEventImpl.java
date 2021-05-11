/*
 * Copyright (C) 2018 Velocity Contributors
 *
 * The Velocity API is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package com.velocitypowered.api.event.player;

import com.google.common.base.Preconditions;
import com.velocitypowered.api.proxy.connection.Player;

/**
 * This event is fired once the player has been fully initialized and is about to connect to their
 * first server.
 */
public final class PostLoginEventImpl implements PostLoginEvent {

  private final Player player;

  public PostLoginEventImpl(Player player) {
    this.player = Preconditions.checkNotNull(player, "player");
  }

  @Override
  public Player player() {
    return player;
  }

  @Override
  public String toString() {
    return "PostLoginEvent{"
        + "player=" + player
        + '}';
  }
}
