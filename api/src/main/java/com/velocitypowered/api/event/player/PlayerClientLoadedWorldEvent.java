/*
 * Copyright (C) 2018-2026 Velocity Contributors
 *
 * The Velocity API is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package com.velocitypowered.api.event.player;

import com.google.common.annotations.Beta;
import com.google.common.base.Preconditions;
import com.velocitypowered.api.proxy.Player;

/**
 * Called when a player is marked as loaded by the client.
 *
 * <p>This event is fired once per {@link com.velocitypowered.api.proxy.ServerConnection}
 * when the player explicitly notifies the server after loading the world (closing the downloading terrain screen)
 *
 * @implNote Unlike Paper this event will <u>not</u> fire due to a timeout nor respawning.
 *     Though plugins can implement a timeout by scheduling a task in {@link ServerPostConnectEvent}
 *     and checking {@link com.velocitypowered.api.proxy.ServerConnection#isClientLoaded()}.
 * @sinceMinecraft 1.21.4
 * @since 4.1.0
 */
@Beta
public final class PlayerClientLoadedWorldEvent {

  private final Player player;

  public PlayerClientLoadedWorldEvent(Player player) {
    this.player = Preconditions.checkNotNull(player, "player");
  }

  public Player getPlayer() {
    return player;
  }

  @Override
  public String toString() {
    return "PlayerClientLoadedWorldEvent{"
        + "player=" + player
        + '}';
  }
}
