/*
 * Copyright (C) 2018 Velocity Contributors
 *
 * The Velocity API is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package com.velocitypowered.api.event.player;

import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;
import com.velocitypowered.api.proxy.connection.Player;
import com.velocitypowered.api.proxy.player.PlayerSettings;

public final class PlayerSettingsChangedEventImpl implements PlayerSettingsChangedEvent {

  private final Player player;
  private final PlayerSettings playerSettings;

  public PlayerSettingsChangedEventImpl(Player player, PlayerSettings playerSettings) {
    this.player = Preconditions.checkNotNull(player, "player");
    this.playerSettings = Preconditions.checkNotNull(playerSettings, "playerSettings");
  }

  @Override
  public Player getPlayer() {
    return player;
  }

  @Override
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
