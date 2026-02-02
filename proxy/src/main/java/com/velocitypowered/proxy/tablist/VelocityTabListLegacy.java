/*
 * Copyright (C) 2019-2023 Velocity Contributors
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

import com.google.common.collect.ImmutableList;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.crypto.IdentifiedKey;
import com.velocitypowered.api.proxy.player.ChatSession;
import com.velocitypowered.api.proxy.player.TabListEntry;
import com.velocitypowered.api.util.GameProfile;
import com.velocitypowered.proxy.connection.client.ConnectedPlayer;
import com.velocitypowered.proxy.protocol.packet.LegacyPlayerListItemPacket;
import com.velocitypowered.proxy.protocol.packet.LegacyPlayerListItemPacket.Item;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.kyori.adventure.text.Component;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Exposes the legacy 1.7 tab list to plugins.
 */
public class VelocityTabListLegacy extends KeyedVelocityTabList {

  private final Map<String, UUID> nameMapping = new ConcurrentHashMap<>();

  public VelocityTabListLegacy(final ConnectedPlayer player, final ProxyServer proxyServer) {
    super(player, proxyServer);
  }

  @Deprecated
  @Override
  public void setHeaderAndFooter(Component header, Component footer) {
  }

  @Override
  public void clearHeaderAndFooter() {
  }

  @Override
  public void addEntry(TabListEntry entry) {
    super.addEntry(entry);
    nameMapping.put(entry.getProfile().getName(), entry.getProfile().getId());
  }

  @Override
  public Optional<TabListEntry> removeEntry(UUID uuid) {
    Optional<TabListEntry> entry = super.removeEntry(uuid);
    entry.map(TabListEntry::getProfile).map(GameProfile::getName).ifPresent(nameMapping::remove);
    return entry;
  }

  @Override
  public void clearAll() {
    for (TabListEntry value : entries.values()) {
      connection.delayedWrite(new LegacyPlayerListItemPacket(
          LegacyPlayerListItemPacket.REMOVE_PLAYER,
          Collections.singletonList(LegacyPlayerListItemPacket.Item.from(value))));
    }
    clearAllSilent();
  }

  @Override
  public void clearAllSilent() {
    entries.clear();
    nameMapping.clear();
  }

  @Override
  public void processLegacy(LegacyPlayerListItemPacket packet) {
    Item item = packet.getItems().getFirst(); // Only one item per packet in 1.7

    switch (packet.getAction()) {
      case LegacyPlayerListItemPacket.ADD_PLAYER:
        if (nameMapping.containsKey(item.getName())) { // ADD_PLAYER also used for updating ping
          KeyedVelocityTabListEntry entry = entries.get(nameMapping.get(item.getName()));
          if (entry != null) {
            entry.setLatencyInternal(item.getLatency());
          }
        } else {
          UUID uuid = UUID.randomUUID(); // Use a fake uuid to preserve function of custom entries
          nameMapping.put(item.getName(), uuid);
          entries.put(uuid, (KeyedVelocityTabListEntry) TabListEntry.builder()
              .tabList(this)
              .profile(new GameProfile(uuid, item.getName(), ImmutableList.of()))
              .latency(item.getLatency())
              .build());
        }
        break;
      case LegacyPlayerListItemPacket.REMOVE_PLAYER:
        UUID removedUuid = nameMapping.remove(item.getName());
        if (removedUuid != null) {
          entries.remove(removedUuid);
        }
        break;
      default:
        // For 1.7 there is only add and remove
        break;
    }
  }

  @Override
  void updateEntry(int action, TabListEntry entry) {
    if (entries.containsKey(entry.getProfile().getId())) {
      switch (action) {
        case LegacyPlayerListItemPacket.UPDATE_LATENCY:
        // Add here because we removed beforehand
        case LegacyPlayerListItemPacket.UPDATE_DISPLAY_NAME:
          connection
              .write(new LegacyPlayerListItemPacket(LegacyPlayerListItemPacket.ADD_PLAYER,
                  // ADD_PLAYER also updates ping
                  Collections.singletonList(LegacyPlayerListItemPacket.Item.from(entry))));
          break;
        default:
          // Can't do anything else
          break;
      }
    }
  }

  @Override
  public TabListEntry buildEntry(GameProfile profile,
                                 net.kyori.adventure.text.@Nullable Component displayName,
                                 int latency, int gameMode, @Nullable IdentifiedKey key) {
    return new VelocityTabListEntryLegacy(this, profile, displayName, latency, gameMode);
  }

  @Override
  public TabListEntry buildEntry(GameProfile profile, @Nullable Component displayName, int latency,
                                 int gameMode, @Nullable ChatSession chatSession, boolean listed) {
    return new VelocityTabListEntryLegacy(this, profile, displayName, latency, gameMode);
  }

  @Override
  public TabListEntry buildEntry(GameProfile profile, @Nullable Component displayName, int latency,
                                 int gameMode, @Nullable ChatSession chatSession, boolean listed, int listOrder, boolean showHat) {
    return new VelocityTabListEntryLegacy(this, profile, displayName, latency, gameMode);
  }
}
