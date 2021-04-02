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
import com.velocitypowered.api.util.GameProfile;
import com.velocitypowered.proxy.protocol.packet.PlayerListItem;
import java.util.Optional;
import net.kyori.adventure.text.serializer.legacytext3.LegacyText3ComponentSerializer;
import net.kyori.text.Component;
import org.checkerframework.checker.nullness.qual.Nullable;

public class VelocityTabListEntry implements TabListEntry {

  private final VelocityTabList tabList;
  private final GameProfile profile;
  private net.kyori.adventure.text.Component displayName;
  private int latency;
  private int gameMode;

  VelocityTabListEntry(VelocityTabList tabList, GameProfile profile,
      net.kyori.adventure.text.@Nullable Component displayName, int latency, int gameMode) {
    this.tabList = tabList;
    this.profile = profile;
    this.displayName = displayName;
    this.latency = latency;
    this.gameMode = gameMode;
  }

  @Override
  public TabList getTabList() {
    return tabList;
  }

  @Override
  public GameProfile getProfile() {
    return profile;
  }

  @Override
  public Optional<Component> getDisplayName() {
    return Optional.ofNullable(displayName).map(LegacyText3ComponentSerializer.get()::serialize);
  }

  @Override
  public Optional<net.kyori.adventure.text.Component> getDisplayNameComponent() {
    return Optional.ofNullable(displayName);
  }

  @Override
  public TabListEntry setDisplayName(@Nullable Component displayName) {
    if (displayName == null) {
      return this.setDisplayName((net.kyori.adventure.text.Component) null);
    }
    return this.setDisplayName(LegacyText3ComponentSerializer.get().deserialize(displayName));
  }

  @Override
  public TabListEntry setDisplayName(net.kyori.adventure.text.@Nullable Component displayName) {
    this.displayName = displayName;
    tabList.updateEntry(PlayerListItem.UPDATE_DISPLAY_NAME, this);
    return this;
  }

  void setDisplayNameInternal(net.kyori.adventure.text.@Nullable Component displayName) {
    this.displayName = displayName;
  }

  @Override
  public int getLatency() {
    return latency;
  }

  @Override
  public TabListEntry setLatency(int latency) {
    this.latency = latency;
    tabList.updateEntry(PlayerListItem.UPDATE_LATENCY, this);
    return this;
  }

  void setLatencyInternal(int latency) {
    this.latency = latency;
  }

  @Override
  public int getGameMode() {
    return gameMode;
  }

  @Override
  public TabListEntry setGameMode(int gameMode) {
    this.gameMode = gameMode;
    tabList.updateEntry(PlayerListItem.UPDATE_GAMEMODE, this);
    return this;
  }

  void setGameModeInternal(int gameMode) {
    this.gameMode = gameMode;
  }
}
