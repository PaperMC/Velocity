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

import com.velocitypowered.api.proxy.player.TabListEntry;
import com.velocitypowered.api.util.GameProfile;
import net.kyori.adventure.text.Component;
import org.checkerframework.checker.nullness.qual.Nullable;

public class VelocityTabListEntryLegacy extends VelocityTabListEntry {

  VelocityTabListEntryLegacy(VelocityTabListLegacy tabList, GameProfile profile,
      @Nullable Component displayName, int latency, int gameMode) {
    super(tabList, profile, displayName, latency, gameMode);
  }

  @Override
  public TabListEntry setDisplayName(net.kyori.text.@Nullable Component displayName) {
    getTabList().removeEntry(getProfile().getId()); // We have to remove first if updating
    return super.setDisplayName(displayName);
  }

  @Override
  public TabListEntry setDisplayName(@Nullable Component displayName) {
    getTabList().removeEntry(getProfile().getId()); // We have to remove first if updating
    return super.setDisplayName(displayName);
  }
}
