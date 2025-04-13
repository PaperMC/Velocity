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
 * <p>Note: This event may be fired in the following scenarios:
 * <br>- If the player notifies the server after loading the world (closing the downloading terrain screen)
 * <br>- If the player has <u>not</u> notified the server within 1500ms after joining the server - ({@link #timeout} = true)
 *
 * <p>Note: Velocity does not ensure the timing of this packet. Consequently, this event might fire before {@link Player#getCurrentServer()} is set.
 */
@Beta
public final class PlayerClientLoadedWorldEvent {

  public static final Duration VANILLA_TIMEOUT = Duration.ofMillis(1500); // 60 ticks

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
   * for 1500ms after the player joined the server.
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