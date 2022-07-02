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

import com.google.common.base.Preconditions;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.crypto.IdentifiedKey;
import com.velocitypowered.api.proxy.player.TabList;
import com.velocitypowered.api.proxy.player.TabListEntry;
import com.velocitypowered.api.util.GameProfile;
import com.velocitypowered.proxy.connection.MinecraftConnection;
import com.velocitypowered.proxy.connection.client.ConnectedPlayer;
import com.velocitypowered.proxy.protocol.packet.HeaderAndFooter;
import com.velocitypowered.proxy.protocol.packet.PlayerListItem;
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

public class VelocityTabList implements TabList {

  protected final ConnectedPlayer player;
  protected final MinecraftConnection connection;
  protected final ProxyServer proxyServer;
  protected final Map<UUID, VelocityTabListEntry> entries = new ConcurrentHashMap<>();

  /**
  * Creates a new VelocityTabList.
  */
  public VelocityTabList(final ConnectedPlayer player, final ProxyServer proxyServer) {
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
    connection.write(HeaderAndFooter.reset());
  }

  @Override
  public void addEntry(TabListEntry entry) {
    Preconditions.checkNotNull(entry, "entry");
    Preconditions.checkArgument(entry.getTabList().equals(this),
        "The provided entry was not created by this tab list");
    Preconditions.checkArgument(!entries.containsKey(entry.getProfile().getId()),
        "this TabList already contains an entry with the same uuid");
    Preconditions.checkArgument(entry instanceof VelocityTabListEntry,
        "Not a Velocity tab list entry");

    PlayerListItem.Item packetItem = PlayerListItem.Item.from(entry);
    connection.write(
        new PlayerListItem(PlayerListItem.ADD_PLAYER, Collections.singletonList(packetItem)));
    entries.put(entry.getProfile().getId(), (VelocityTabListEntry) entry);
  }

  @Override
  public Optional<TabListEntry> removeEntry(UUID uuid) {
    Preconditions.checkNotNull(uuid, "uuid");

    TabListEntry entry = entries.remove(uuid);
    if (entry != null) {
      PlayerListItem.Item packetItem = PlayerListItem.Item.from(entry);
      connection.write(
          new PlayerListItem(PlayerListItem.REMOVE_PLAYER, Collections.singletonList(packetItem)));
    }

    return Optional.ofNullable(entry);
  }

  @Override
  public boolean containsEntry(UUID uuid) {
    Preconditions.checkNotNull(uuid, "uuid");
    return entries.containsKey(uuid);
  }

  /**
   * Clears all entries from the tab list. Note that the entries are written with {@link
   * MinecraftConnection#delayedWrite(Object)}, so make sure to do an explicit {@link
   * MinecraftConnection#flush()}.
   */
  public void clearAll() {
    Collection<VelocityTabListEntry> listEntries = entries.values();
    if (listEntries.isEmpty()) {
      return;
    }
    List<PlayerListItem.Item> items = new ArrayList<>(listEntries.size());
    for (TabListEntry value : listEntries) {
      items.add(PlayerListItem.Item.from(value));
    }
    entries.clear();
    connection.delayedWrite(new PlayerListItem(PlayerListItem.REMOVE_PLAYER, items));
  }

  @Override
  public Collection<TabListEntry> getEntries() {
    return Collections.unmodifiableCollection(this.entries.values());
  }

  @Override
  public TabListEntry buildEntry(GameProfile profile, @Nullable Component displayName, int latency, int gameMode) {
    return buildEntry(profile, displayName, latency, gameMode, null);
  }

  @Override
  public TabListEntry buildEntry(GameProfile profile,
                                 net.kyori.adventure.text.@Nullable Component displayName,
                                 int latency, int gameMode, @Nullable IdentifiedKey key) {
    return new VelocityTabListEntry(this, profile, displayName, latency, gameMode, key);
  }

  /**
   * Processes a tab list entry packet from the backend.
   *
   * @param packet the packet to process
   */
  public void processBackendPacket(PlayerListItem packet) {
    // Packets are already forwarded on, so no need to do that here
    for (PlayerListItem.Item item : packet.getItems()) {
      UUID uuid = item.getUuid();
      assert uuid != null : "1.7 tab list entry given to modern tab list handler!";

      if (packet.getAction() != PlayerListItem.ADD_PLAYER && !entries.containsKey(uuid)) {
        // Sometimes UPDATE_GAMEMODE is sent before ADD_PLAYER so don't want to warn here
        continue;
      }

      switch (packet.getAction()) {
        case PlayerListItem.ADD_PLAYER: {
          // ensure that name and properties are available
          String name = item.getName();
          List<GameProfile.Property> properties = item.getProperties();
          if (name == null || properties == null) {
            throw new IllegalStateException("Got null game profile for ADD_PLAYER");
          }
          // Verify key
          IdentifiedKey providedKey = item.getPlayerKey();
          Optional<Player> connected = proxyServer.getPlayer(uuid);
          if (connected.isPresent()) {
            IdentifiedKey expectedKey = connected.get().getIdentifiedKey();
            if (providedKey != null) {
              if (!Objects.equals(expectedKey, providedKey)) {
                throw new IllegalStateException("Server provided incorrect player key in playerlist for "
                        + name + " UUID: " + uuid);
              }
            } else {
              // Substitute the key
              // It shouldn't be propagated to remove the signature.
              providedKey = expectedKey;
            }
          }

          entries.putIfAbsent(item.getUuid(), (VelocityTabListEntry) TabListEntry.builder()
              .tabList(this)
              .profile(new GameProfile(uuid, name, properties))
              .displayName(item.getDisplayName())
              .latency(item.getLatency())
              .playerKey(providedKey)
              .gameMode(item.getGameMode())
              .build());
          break;
        }
        case PlayerListItem.REMOVE_PLAYER:
          entries.remove(uuid);
          break;
        case PlayerListItem.UPDATE_DISPLAY_NAME: {
          VelocityTabListEntry entry = entries.get(uuid);
          if (entry != null) {
            entry.setDisplayNameInternal(item.getDisplayName());
          }
          break;
        }
        case PlayerListItem.UPDATE_LATENCY: {
          VelocityTabListEntry entry = entries.get(uuid);
          if (entry != null) {
            entry.setLatencyInternal(item.getLatency());
          }
          break;
        }
        case PlayerListItem.UPDATE_GAMEMODE: {
          VelocityTabListEntry entry = entries.get(uuid);
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
      PlayerListItem.Item packetItem = PlayerListItem.Item.from(entry);

      Optional<Player> existing = proxyServer.getPlayer(entry.getProfile().getId());
      existing.ifPresent(value -> packetItem.setPlayerKey(value.getIdentifiedKey()));

      connection.write(new PlayerListItem(action, Collections.singletonList(packetItem)));
    }
  }
}
