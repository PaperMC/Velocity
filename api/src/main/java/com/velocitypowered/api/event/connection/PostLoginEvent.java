/*
 * Copyright (C) 2018-2021 Velocity Contributors
 *
 * The Velocity API is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package com.velocitypowered.api.event.connection;

import com.google.common.base.Preconditions;
import com.velocitypowered.api.event.annotation.AwaitingEvent;
import com.velocitypowered.api.proxy.Player;

/**
 * This event is fired once the player has been fully initialized and is about to connect to their
 * first server. Velocity will wait for this event to finish firing before it fires
 * {@link com.velocitypowered.api.event.player.PlayerChooseInitialServerEvent} with any default
 * servers specified in the configuration, but you should try to limit the work done in any event
 * that fires during the login process.
 */
@AwaitingEvent
public final class PostLoginEvent {

  private final Player player;

  /**
   * Constructs a new PostLoginEvent.
   *
   * @param player the player who has logged in
   */
  public PostLoginEvent(Player player) {
    this.player = Preconditions.checkNotNull(player, "player");
  }

  /**
   * Gets the player who has logged in and is about to connect to their first server.
   *
   * @return the player
   */
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
