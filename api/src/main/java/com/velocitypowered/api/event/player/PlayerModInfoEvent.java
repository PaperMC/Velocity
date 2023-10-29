/*
 * Copyright (C) 2018-2021 Velocity Contributors
 *
 * The Velocity API is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package com.velocitypowered.api.event.player;

import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;
import com.velocitypowered.api.proxy.player.Player;
import com.velocitypowered.api.util.ModInfo;

/**
 * This event is fired when a Forge client sends its mods to the proxy while connecting to a server.
 * Velocity will not wait on this event to finish firing.
 */
public final class PlayerModInfoEvent implements PlayerReferentEvent {

  private final Player player;
  private final ModInfo modInfo;

  public PlayerModInfoEvent(Player player, ModInfo modInfo) {
    this.player = Preconditions.checkNotNull(player, "player");
    this.modInfo = Preconditions.checkNotNull(modInfo, "modInfo");
  }

  @Override
  public Player player() {
    return player;
  }

  public ModInfo modInfo() {
    return modInfo;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("player", player)
        .add("modInfo", modInfo)
        .toString();
  }
}
