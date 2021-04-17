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
import com.velocitypowered.api.proxy.player.ClientSettings;

public final class PlayerClientSettingsChangedEventImpl implements
    PlayerClientSettingsChangedEvent {

  private final Player player;
  private final ClientSettings clientSettings;

  public PlayerClientSettingsChangedEventImpl(Player player, ClientSettings clientSettings) {
    this.player = Preconditions.checkNotNull(player, "player");
    this.clientSettings = Preconditions.checkNotNull(clientSettings, "playerSettings");
  }

  @Override
  public Player player() {
    return player;
  }

  @Override
  public ClientSettings settings() {
    return clientSettings;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("player", player)
        .add("playerSettings", clientSettings)
        .toString();
  }
}
