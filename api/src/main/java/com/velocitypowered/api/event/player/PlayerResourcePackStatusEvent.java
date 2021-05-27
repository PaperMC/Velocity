/*
 * Copyright (C) 2018 Velocity Contributors
 *
 * The Velocity API is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package com.velocitypowered.api.event.player;

import com.velocitypowered.api.event.Event;
import com.velocitypowered.api.proxy.connection.Player;

/**
 * This event is fired when the status of a resource pack sent to the player by the server is
 * changed.
 */
public interface PlayerResourcePackStatusEvent extends Event {

  /**
   * Returns the player affected by the change in resource pack status.
   *
   * @return the player
   */
  Player player();

  /**
   * Returns the new status for the resource pack.
   *
   * @return the new status
   */
  Status status();

  /**
   * Represents the possible statuses for the resource pack.
   */
  enum Status {
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
