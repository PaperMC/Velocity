/*
 * Copyright (C) 2018-2025 Velocity Contributors
 *
 * The Velocity API is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package com.velocitypowered.api.event.player;

import com.google.common.annotations.Beta;
import com.google.common.base.Preconditions;
import com.velocitypowered.api.proxy.Player;
import java.time.Duration;

/**
 * Called when a player is marked as loaded by the client.
 *
 * <p>This event may be fired in the following scenarios:
 * <br>- If the player notifies the server after loading the world (closing the downloading terrain screen)
 * <br>- If the player has <u>not</u> notified the server within {@link PlayerClientLoadedWorldEvent#TIMEOUT} after joining the server
 * - ({@link #timeout} = true)
 *
 * @apiNote Velocity does not ensure the timing of this packet. Consequently, this event might fire before {@link Player#getCurrentServer()} is set.
 *     <br>To (not) let other plugins override the default 1500ms,
 *     (don't) load the class, override the value yourself, and/or ensure its value later on.
 * @since 3.4.0
 * @sinceMinecraft 1.21.4
 */
@Beta
public final class PlayerClientLoadedWorldEvent {

  public static final Duration TIMEOUT = Duration.ofMillis(Long.getLong("velocity.loaded-world-timeout-override", 1500 /* 60 ticks */));

  private final Player player;
  private final boolean timeout;

  public PlayerClientLoadedWorldEvent(Player player, boolean timeout) {
    this.player = Preconditions.checkNotNull(player, "player");
    this.timeout = timeout;
  }

  public Player getPlayer() {
    return player;
  }

  /**
   * True if the event was triggered because the server has not been notified by the player
   * withing {@link PlayerClientLoadedWorldEvent#TIMEOUT} after the player joined the server.
   *
   * @return true if the event was triggered because of a timeout
   */
  public boolean isTimeout() {
    return timeout;
  }

  @Override
  public String toString() {
    return "PlayerClientLoadedWorldEvent{"
        + "player=" + player
        + ", timeout=" + timeout
        + '}';
  }

}