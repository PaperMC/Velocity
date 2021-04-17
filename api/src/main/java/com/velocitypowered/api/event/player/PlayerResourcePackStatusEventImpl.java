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
 * This event is fired when the status of a resource pack sent to the player by the server is
 * changed.
 */
public class PlayerResourcePackStatusEventImpl implements PlayerResourcePackStatusEvent {

  private final Player player;
  private final Status status;

  public PlayerResourcePackStatusEventImpl(Player player, Status status) {
    this.player = Preconditions.checkNotNull(player, "player");
    this.status = Preconditions.checkNotNull(status, "status");
  }

  /**
   * Returns the player affected by the change in resource pack status.
   *
   * @return the player
   */
  @Override
  public Player player() {
    return player;
  }

  /**
   * Returns the new status for the resource pack.
   *
   * @return the new status
   */
  @Override
  public Status status() {
    return status;
  }

  @Override
  public String toString() {
    return "PlayerResourcePackStatusEvent{"
        + "player=" + player
        + ", status=" + status
        + '}';
  }

}
