/*
 * Copyright (C) 2018 Velocity Contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.velocitypowered.proxy.tablist;

import com.velocitypowered.api.proxy.player.TabList;
import com.velocitypowered.api.proxy.player.TabListEntry;
import com.velocitypowered.api.proxy.player.java.JavaPlayerIdentity;
import com.velocitypowered.proxy.network.java.packet.clientbound.ClientboundPlayerListItemPacket;
import net.kyori.adventure.text.Component;
import org.checkerframework.checker.nullness.qual.Nullable;

public class VelocityTabListEntry implements TabListEntry {

  private final VelocityTabList tabList;
  private final JavaPlayerIdentity profile;
  private @Nullable Component displayName;
  private int latency;
  private int gameMode;

  VelocityTabListEntry(VelocityTabList tabList, JavaPlayerIdentity profile,
      @Nullable Component displayName, int latency, int gameMode) {
    this.tabList = tabList;
    this.profile = profile;
    this.displayName = displayName;
    this.latency = latency;
    this.gameMode = gameMode;
  }

  @Override
  public TabList parent() {
    return tabList;
  }

  @Override
  public JavaPlayerIdentity gameProfile() {
    return profile;
  }

  @Override
  public @Nullable Component displayName() {
    return displayName;
  }

  @Override
  public TabListEntry setDisplayName(@Nullable Component displayName) {
    this.displayName = displayName;
    tabList.updateEntry(ClientboundPlayerListItemPacket.UPDATE_DISPLAY_NAME, this);
    return this;
  }

  void setDisplayNameInternal(@Nullable Component displayName) {
    this.displayName = displayName;
  }

  @Override
  public int ping() {
    return latency;
  }

  @Override
  public TabListEntry setPing(int latency) {
    this.latency = latency;
    tabList.updateEntry(ClientboundPlayerListItemPacket.UPDATE_LATENCY, this);
    return this;
  }

  void setLatencyInternal(int latency) {
    this.latency = latency;
  }

  @Override
  public int gameMode() {
    return gameMode;
  }

  @Override
  public TabListEntry setGameMode(int gameMode) {
    this.gameMode = gameMode;
    tabList.updateEntry(ClientboundPlayerListItemPacket.UPDATE_GAMEMODE, this);
    return this;
  }

  void setGameModeInternal(int gameMode) {
    this.gameMode = gameMode;
  }
}
