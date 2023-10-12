/*
 * Copyright (C) 2018-2023 Velocity Contributors
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

import com.google.common.base.Preconditions;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.crypto.IdentifiedKey;
import com.velocitypowered.api.proxy.player.ChatSession;
import com.velocitypowered.api.proxy.player.TabListEntry;
import com.velocitypowered.api.util.GameProfile;
import com.velocitypowered.proxy.connection.MinecraftConnection;
import com.velocitypowered.proxy.connection.client.ConnectedPlayer;
import com.velocitypowered.proxy.protocol.packet.LegacyPlayerListItem;
import com.velocitypowered.proxy.protocol.packet.chat.RemoteChatSession;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.kyori.adventure.text.Component;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Exposes the tab list to plugins.
 */
public class KeyedVelocityTabList implements InternalTabList {

  protected final ConnectedPlayer player;
  protected final MinecraftConnection connection;
  protected final ProxyServer proxyServer;
  protected final Map<UUID, KeyedVelocityTabListEntry> entries = new ConcurrentHashMap<>();

  /**
   * Creates a new VelocityTabList.
   */
  public KeyedVelocityTabList(final ConnectedPlayer player, final ProxyServer proxyServer) {
    this.player = player;
    this.proxyServer = proxyServer;
    this.connection = player.getConnection();
  }

  @Deprecated
  @Override
  public void setHeaderAndFooter(Component header, Component footer) {
    Preconditions.checkNotNull(header, "header");
    Preconditions.checkNotNull(footer, "footer");
    this.player.sendPlayerListHeaderAndFooter(header, footer);
  }

  @Override
  public void clearHeaderAndFooter() {
    this.player.clearPlayerListHeaderAndFooter();
  }

  @Override
  public void addEntry(TabListEntry entry) {
    Preconditions.checkNotNull(entry, "entry");
    Preconditions.checkArgument(entry.getTabList().equals(this),
        "The provided entry was not created by this tab list");
    Preconditions.checkArgument(!entries.containsKey(entry.getProfile().getId()),
        "this TabList already contains an entry with the same uuid");
    Preconditions.checkArgument(entry instanceof KeyedVelocityTabListEntry,
        "Not a Velocity tab list entry");

    LegacyPlayerListItem.Item packetItem = LegacyPlayerListItem.Item.from(entry);
    connection.write(
        new LegacyPlayerListItem(LegacyPlayerListItem.ADD_PLAYER,
            Collections.singletonList(packetItem)));
    entries.put(entry.getProfile().getId(), (KeyedVelocityTabListEntry) entry);
  }

  @Override
  public Optional<TabListEntry> removeEntry(UUID uuid) {
    Preconditions.checkNotNull(uuid, "uuid");

    TabListEntry entry = entries.remove(uuid);
    if (entry != null) {
      LegacyPlayerListItem.Item packetItem = LegacyPlayerListItem.Item.from(entry);
      connection.write(
          new LegacyPlayerListItem(LegacyPlayerListItem.REMOVE_PLAYER,
              Collections.singletonList(packetItem)));
    }

    return Optional.ofNullable(entry);
  }

  @Override
  public boolean containsEntry(UUID uuid) {
    Preconditions.checkNotNull(uuid, "uuid");
    return entries.containsKey(uuid);
  }

  @Override
  public Optional<TabListEntry> getEntry(UUID uuid) {
    return Optional.ofNullable(this.entries.get(uuid));
  }

  /**
   * Clears all entries from the tab list. Note that the entries are written with
   * {@link MinecraftConnection#delayedWrite(Object)}, so make sure to do an explicit
   * {@link MinecraftConnection#flush()}.
   */
  @Override
  public void clearAll() {
    Collection<KeyedVelocityTabListEntry> listEntries = entries.values();
    if (listEntries.isEmpty()) {
      return;
    }
    List<LegacyPlayerListItem.Item> items = new ArrayList<>(listEntries.size());
    for (TabListEntry value : listEntries) {
      items.add(LegacyPlayerListItem.Item.from(value));
    }
    clearAllSilent();
    connection.delayedWrite(new LegacyPlayerListItem(LegacyPlayerListItem.REMOVE_PLAYER, items));
  }

  @Override
  public void clearAllSilent() {
    entries.clear();
  }

  @Override
  public Collection<TabListEntry> getEntries() {
    return Collections.unmodifiableCollection(this.entries.values());
  }

  @Override
  public TabListEntry buildEntry(GameProfile profile,
      net.kyori.adventure.text.@Nullable Component displayName,
      int latency, int gameMode, @Nullable IdentifiedKey key) {
    return new KeyedVelocityTabListEntry(this, profile, displayName, latency, gameMode, key);
  }

  @Override
  public TabListEntry buildEntry(GameProfile profile, @Nullable Component displayName, int latency,
      int gameMode,
      @Nullable ChatSession chatSession, boolean listed) {
    return new KeyedVelocityTabListEntry(this, profile, displayName, latency, gameMode,
        chatSession == null ? null : chatSession.getIdentifiedKey());
  }

  @Override
  public void processLegacy(LegacyPlayerListItem packet) {
    // Packets are already forwarded on, so no need to do that here
    for (LegacyPlayerListItem.Item item : packet.getItems()) {
      UUID uuid = item.getUuid();
      assert uuid != null : "1.7 tab list entry given to modern tab list handler!";

      if (packet.getAction() != LegacyPlayerListItem.ADD_PLAYER && !entries.containsKey(uuid)) {
        // Sometimes UPDATE_GAMEMODE is sent before ADD_PLAYER so don't want to warn here
        continue;
      }

      switch (packet.getAction()) {
        case LegacyPlayerListItem.ADD_PLAYER: {
          // ensure that name and properties are available
          String name = item.getName();
          List<GameProfile.Property> properties = item.getProperties();
          if (name == null || properties == null) {
            throw new IllegalStateException("Got null game profile for ADD_PLAYER");
          }

          entries.putIfAbsent(item.getUuid(), (KeyedVelocityTabListEntry) TabListEntry.builder()
              .tabList(this)
              .profile(new GameProfile(uuid, name, properties))
              .displayName(item.getDisplayName())
              .latency(item.getLatency())
              .chatSession(new RemoteChatSession(null, item.getPlayerKey()))
              .gameMode(item.getGameMode())
              .build());
          break;
        }
        case LegacyPlayerListItem.REMOVE_PLAYER:
          entries.remove(uuid);
          break;
        case LegacyPlayerListItem.UPDATE_DISPLAY_NAME: {
          KeyedVelocityTabListEntry entry = entries.get(uuid);
          if (entry != null) {
            entry.setDisplayNameInternal(item.getDisplayName());
          }
          break;
        }
        case LegacyPlayerListItem.UPDATE_LATENCY: {
          KeyedVelocityTabListEntry entry = entries.get(uuid);
          if (entry != null) {
            entry.setLatencyInternal(item.getLatency());
          }
          break;
        }
        case LegacyPlayerListItem.UPDATE_GAMEMODE: {
          KeyedVelocityTabListEntry entry = entries.get(uuid);
          if (entry != null) {
            entry.setGameModeInternal(item.getGameMode());
          }
          break;
        }
        default:
          // Nothing we can do here
          break;
      }
    }
  }

  void updateEntry(int action, TabListEntry entry) {
    if (entries.containsKey(entry.getProfile().getId())) {
      LegacyPlayerListItem.Item packetItem = LegacyPlayerListItem.Item.from(entry);

      IdentifiedKey selectedKey = packetItem.getPlayerKey();
      Optional<Player> existing = proxyServer.getPlayer(entry.getProfile().getId());
      if (existing.isPresent()) {
        selectedKey = existing.get().getIdentifiedKey();
      }

      if (selectedKey != null
          && selectedKey.getKeyRevision().getApplicableTo()
          .contains(connection.getProtocolVersion())
          && Objects.equals(selectedKey.getSignatureHolder(), entry.getProfile().getId())) {
        packetItem.setPlayerKey(selectedKey);
      } else {
        packetItem.setPlayerKey(null);
      }

      connection.write(new LegacyPlayerListItem(action, Collections.singletonList(packetItem)));
    }
  }
}
