package com.velocitypowered.proxy.tablist;

import com.velocitypowered.api.proxy.player.TabList;
import com.velocitypowered.api.proxy.player.TabListEntry;
import com.velocitypowered.api.util.AdventureCompat;
import com.velocitypowered.api.util.GameProfile;
import com.velocitypowered.proxy.protocol.packet.PlayerListItem;
import java.util.Optional;
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
    return Optional.ofNullable(displayName).map(AdventureCompat::asOriginalTextComponent);
  }

  @Override
  public Optional<net.kyori.adventure.text.Component> getDisplayNameComponent() {
    return Optional.ofNullable(displayName);
  }

  @Override
  public TabListEntry setDisplayName(@Nullable Component displayName) {
    return this.setDisplayName(AdventureCompat.asAdventureComponent(displayName));
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
