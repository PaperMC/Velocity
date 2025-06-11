/*
 * Copyright (C) 2018-2021 Velocity Contributors
 *
 * The Velocity API is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package com.velocitypowered.api.event.player;

import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.player.PlayerSettings;

/**
 * This event is fired when the client sends new client settings for the player. This event can
 * and typically will be fired multiple times per connection. Velocity will not wait on this event
 * to finish firing.
 */
public final class PlayerSettingsChangedEvent {

  private final Player player;
  private final PlayerSettings playerSettings;

  public PlayerSettingsChangedEvent(Player player, PlayerSettings playerSettings) {
    this.player = Preconditions.checkNotNull(player, "player");
    this.playerSettings = Preconditions.checkNotNull(playerSettings, "playerSettings");
  }

  public Player getPlayer() {
    return player;
  }

  public PlayerSettings getPlayerSettings() {
    return playerSettings;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("player", player)
        .add("playerSettings", playerSettings)
        .toString();
  }
}
