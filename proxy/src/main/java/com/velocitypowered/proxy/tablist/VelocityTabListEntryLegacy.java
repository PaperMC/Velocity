package com.velocitypowered.proxy.tablist;

import com.velocitypowered.api.proxy.player.TabListEntry;
import com.velocitypowered.api.util.GameProfile;
import net.kyori.text.Component;
import org.checkerframework.checker.nullness.qual.Nullable;

public class VelocityTabListEntryLegacy extends VelocityTabListEntry {

  VelocityTabListEntryLegacy(VelocityTabListLegacy tabList, GameProfile profile,
      @Nullable Component displayName, int latency, int gameMode) {
    super(tabList, profile, displayName, latency, gameMode);
  }

  @Override
  public TabListEntry setDisplayName(@Nullable Component displayName) {
    getTabList().removeEntry(getProfile().getId()); // We have to remove first if updating
    return super.setDisplayName(displayName);
  }
}
