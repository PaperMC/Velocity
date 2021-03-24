/*
 * Copyright (C) 2018 Velocity Contributors
 *
 * The Velocity API is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package com.velocitypowered.api.event.player;

import com.google.common.base.Preconditions;
import com.velocitypowered.api.proxy.Player;

/**
 * This event is fired when the status of a resource pack sent to the player by the server is
 * changed.
 */
public class PlayerResourcePackStatusEvent {

  private final Player player;
  private final Status status;

  public PlayerResourcePackStatusEvent(Player player, Status status) {
    this.player = Preconditions.checkNotNull(player, "player");
    this.status = Preconditions.checkNotNull(status, "status");
  }

  /**
   * Returns the player affected by the change in resource pack status.
   *
   * @return the player
   */
  public Player getPlayer() {
    return player;
  }

  /**
   * Returns the new status for the resource pack.
   *
   * @return the new status
   */
  public Status getStatus() {
    return status;
  }

  @Override
  public String toString() {
    return "PlayerResourcePackStatusEvent{"
        + "player=" + player
        + ", status=" + status
        + '}';
  }

  /**
   * Represents the possible statuses for the resource pack.
   */
  public enum Status {
    /**
     * The resource pack was applied successfully.
     */
    SUCCESSFUL,
    /**
     * The player declined to download the resource pack.
     */
    DECLINED,
    /**
     * The player could not download the resource pack.
     */
    FAILED_DOWNLOAD,
    /**
     * The player has accepted the resource pack and is now downloading it.
     */
    ACCEPTED
  }
}
