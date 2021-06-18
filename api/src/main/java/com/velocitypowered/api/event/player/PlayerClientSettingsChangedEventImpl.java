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
import com.velocitypowered.api.proxy.player.java.JavaClientSettings;

public final class PlayerClientSettingsChangedEventImpl implements
    PlayerClientSettingsChangedEvent {

  private final Player player;
  private final JavaClientSettings javaClientSettings;

  public PlayerClientSettingsChangedEventImpl(Player player, JavaClientSettings javaClientSettings) {
    this.player = Preconditions.checkNotNull(player, "player");
    this.javaClientSettings = Preconditions.checkNotNull(javaClientSettings, "playerSettings");
  }

  @Override
  public Player player() {
    return player;
  }

  @Override
  public JavaClientSettings settings() {
    return javaClientSettings;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("player", player)
        .add("playerSettings", javaClientSettings)
        .toString();
  }
}
