package com.velocitypowered.api.event.player;

import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;
import com.velocitypowered.api.proxy.connection.Player;
import com.velocitypowered.api.proxy.player.PlayerSettings;

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
