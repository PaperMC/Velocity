/*
 * Copyright (C) 2018-2024 Velocity Contributors
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

import com.velocitypowered.api.event.player.ServerUpdateTabListEvent;
import com.velocitypowered.api.proxy.player.ChatSession;
import com.velocitypowered.api.proxy.player.TabList;
import com.velocitypowered.api.proxy.player.TabListEntry;
import com.velocitypowered.api.util.GameProfile;
import java.util.Optional;
import net.kyori.adventure.text.Component;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Represents a {@link TabListEntry} of the {@link ServerUpdateTabListEvent}.
 */
public class UpdateEventTabListEntry implements TabListEntry {

  private final TabList tabList;
  private final GameProfile profile;
  private @Nullable Component displayName;
  private int latency;
  private int gameMode;
  private boolean listed;
  private @Nullable ChatSession session;
  private boolean rewrite = false;

  /**
   * Constructs an instance.
   */
  public UpdateEventTabListEntry(TabList tabList, GameProfile profile, @Nullable Component displayName,
                                 int latency,
                                 int gameMode, @Nullable ChatSession session, boolean listed) {
    this.tabList = tabList;
    this.profile = profile;
    this.displayName = displayName;
    this.latency = latency;
    this.gameMode = gameMode;
    this.session = session;
    this.listed = listed;
  }

  @Override
  public @Nullable ChatSession getChatSession() {
    return this.session;
  }

  @Override
  public TabList getTabList() {
    return this.tabList;
  }

  @Override
  public GameProfile getProfile() {
    return this.profile;
  }

  @Override
  public Optional<Component> getDisplayNameComponent() {
    return Optional.ofNullable(displayName);
  }

  @Override
  public TabListEntry setDisplayName(@Nullable Component displayName) {
    this.displayName = displayName;
    rewrite = true;
    return this;
  }

  void setDisplayNameWithoutRewrite(@Nullable Component displayName) {
    this.displayName = displayName;
  }

  @Override
  public int getLatency() {
    return this.latency;
  }

  @Override
  public TabListEntry setLatency(int latency) {
    this.latency = latency;
    rewrite = true;
    return this;
  }

  void setLatencyWithoutRewrite(int latency) {
    this.latency = latency;
  }

  @Override
  public int getGameMode() {
    return this.gameMode;
  }

  @Override
  public TabListEntry setGameMode(int gameMode) {
    this.gameMode = gameMode;
    rewrite = true;
    return this;
  }

  void setGameModeWithoutRewrite(int gameMode) {
    this.gameMode = gameMode;
  }

  void setChatSessionWithoutRewrite(@Nullable ChatSession session) {
    this.session = session;
  }

  @Override
  public boolean isListed() {
    return listed;
  }

  @Override
  public TabListEntry setListed(boolean listed) {
    this.listed = listed;
    rewrite = true;
    return this;
  }

  void setListedWithoutRewrite(boolean listed) {
    this.listed = listed;
  }

  boolean isRewrite() {
    return rewrite;
  }

}
