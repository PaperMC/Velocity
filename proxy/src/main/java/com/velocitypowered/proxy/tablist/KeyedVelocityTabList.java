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
import com.velocitypowered.api.event.player.ServerUpdateTabListEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.crypto.IdentifiedKey;
import com.velocitypowered.api.proxy.player.ChatSession;
import com.velocitypowered.api.proxy.player.TabListEntry;
import com.velocitypowered.api.util.GameProfile;
import com.velocitypowered.proxy.connection.MinecraftConnection;
import com.velocitypowered.proxy.connection.client.ConnectedPlayer;
import com.velocitypowered.proxy.protocol.packet.LegacyPlayerListItemPacket;
import com.velocitypowered.proxy.protocol.packet.chat.RemoteChatSession;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
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

  @Override
  public Player getPlayer() {
    return player;
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
  public void addEntry(TabListEntry entry1) {
    KeyedVelocityTabListEntry entry;
    if (entry1 instanceof KeyedVelocityTabListEntry) {
      entry = (KeyedVelocityTabListEntry) entry1;
    } else {
      entry = new KeyedVelocityTabListEntry(this, entry1.getProfile(),
          entry1.getDisplayNameComponent().orElse(null),
          entry1.getLatency(), entry1.getGameMode(), entry1.getIdentifiedKey());
    }

    Preconditions.checkNotNull(entry, "entry");
    Preconditions.checkArgument(entry.getTabList().equals(this),
        "The provided entry was not created by this tab list");
    Preconditions.checkArgument(!entries.containsKey(entry.getProfile().getId()),
        "this TabList already contains an entry with the same uuid");

    LegacyPlayerListItemPacket.Item packetItem = LegacyPlayerListItemPacket.Item.from(entry);
    connection.write(
        new LegacyPlayerListItemPacket(LegacyPlayerListItemPacket.ADD_PLAYER,
            Collections.singletonList(packetItem)));
    entries.put(entry.getProfile().getId(), entry);
  }

  @Override
  public Optional<TabListEntry> removeEntry(UUID uuid) {
    Preconditions.checkNotNull(uuid, "uuid");

    TabListEntry entry = entries.remove(uuid);
    if (entry != null) {
      LegacyPlayerListItemPacket.Item packetItem = LegacyPlayerListItemPacket.Item.from(entry);
      connection.write(
          new LegacyPlayerListItemPacket(LegacyPlayerListItemPacket.REMOVE_PLAYER,
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
    List<LegacyPlayerListItemPacket.Item> items = new ArrayList<>(listEntries.size());
    for (TabListEntry value : listEntries) {
      items.add(LegacyPlayerListItemPacket.Item.from(value));
    }
    clearAllSilent();
    connection.delayedWrite(new LegacyPlayerListItemPacket(
            LegacyPlayerListItemPacket.REMOVE_PLAYER, items));
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
  public void processLegacyUpdate(LegacyPlayerListItemPacket packet) {
    ServerUpdateTabListEvent.Action action = mapToEventAction(packet.getAction());
    Preconditions.checkNotNull(action, "action");

    List<UpdateEventTabListEntry> entries = mapToEventEntries(packet.getAction(), packet.getItems());

    proxyServer.getEventManager().fire(
        new ServerUpdateTabListEvent(
            player,
            Set.of(action),
            Collections.unmodifiableList(entries)
        )
    ).thenAccept(event -> {
      if (event.getResult().isAllowed()) {
        if (event.getResult().getIds().isEmpty()) {
          boolean rewrite = false;
          for (UpdateEventTabListEntry entry : entries) {
            if (entry.isRewrite()) {
              rewrite = true;
              break;
            }
          }

          if (rewrite) {
            //listeners have modified entries, requires manual processing
            if (action != ServerUpdateTabListEvent.Action.REMOVE_PLAYER) {
              for (UpdateEventTabListEntry entry : entries) {
                if (this.entries.containsKey(entry.getProfile().getId())) {
                  removeEntry(entry.getProfile().getId());
                }

                addEntry(entry);
              }
            } else {
              for (UpdateEventTabListEntry entry : entries) {
                removeEntry(entry.getProfile().getId());
              }
            }
          } else {
            //listeners haven't modified entries
            for (LegacyPlayerListItemPacket.Item item : packet.getItems()) {
              processLegacy(packet.getAction(), item);
            }

            connection.write(packet);
          }
        } else {
          //listeners have denied entries (and may have modified others), requires manual processing
          if (action != ServerUpdateTabListEvent.Action.REMOVE_PLAYER) {
            for (UpdateEventTabListEntry entry : entries) {
              if (event.getResult().getIds().contains(entry.getProfile().getId())) {
                if (this.entries.containsKey(entry.getProfile().getId())) {
                  removeEntry(entry.getProfile().getId());
                }

                addEntry(entry);
              }
            }
          } else {
            for (UpdateEventTabListEntry entry : entries) {
              if (event.getResult().getIds().contains(entry.getProfile().getId())) {
                removeEntry(entry.getProfile().getId());
              }
            }
          }
        }
      }
    }).join();
  }

  protected ServerUpdateTabListEvent.@Nullable Action mapToEventAction(int action) {
    return switch (action) {
      case LegacyPlayerListItemPacket.ADD_PLAYER -> ServerUpdateTabListEvent.Action.ADD_PLAYER;
      case LegacyPlayerListItemPacket.REMOVE_PLAYER -> ServerUpdateTabListEvent.Action.REMOVE_PLAYER;
      case LegacyPlayerListItemPacket.UPDATE_GAMEMODE -> ServerUpdateTabListEvent.Action.UPDATE_GAME_MODE;
      case LegacyPlayerListItemPacket.UPDATE_LATENCY -> ServerUpdateTabListEvent.Action.UPDATE_LATENCY;
      case LegacyPlayerListItemPacket.UPDATE_DISPLAY_NAME -> ServerUpdateTabListEvent.Action.UPDATE_DISPLAY_NAME;
      default -> null;
    };
  }

  private List<UpdateEventTabListEntry> mapToEventEntries(int action, List<LegacyPlayerListItemPacket.Item> packetItems) {
    List<UpdateEventTabListEntry> entries = new ArrayList<>(packetItems.size());

    for (LegacyPlayerListItemPacket.Item item : packetItems) {
      UUID uuid = item.getUuid();
      Preconditions.checkNotNull(uuid, "1.7 tab list entry given to modern tab list handler!");

      if (action != LegacyPlayerListItemPacket.ADD_PLAYER
          && !this.entries.containsKey(uuid)) {
        // Sometimes UPDATE_GAMEMODE is sent before ADD_PLAYER so don't want to warn here
        continue;
      }

      UpdateEventTabListEntry currentEntry = null;
      KeyedVelocityTabListEntry oldCurrentEntry = this.entries.get(uuid);

      if (oldCurrentEntry != null) {
        currentEntry = new UpdateEventTabListEntry(
            this,
            oldCurrentEntry.getProfile(),
            oldCurrentEntry.getDisplayNameComponent().orElse(null),
            oldCurrentEntry.getLatency(),
            oldCurrentEntry.getGameMode(),
            oldCurrentEntry.getChatSession(),
            oldCurrentEntry.isListed()
        );
      }

      switch (action) {
        case LegacyPlayerListItemPacket.ADD_PLAYER -> {
          // ensure that name and properties are available
          String name = item.getName();
          List<GameProfile.Property> properties = item.getProperties();
          if (name == null || properties == null) {
            throw new IllegalStateException("Got null game profile for ADD_PLAYER");
          }

          currentEntry = new UpdateEventTabListEntry(
              this,
              new GameProfile(uuid, name, properties),
              item.getDisplayName(),
              item.getLatency(),
              item.getGameMode(),
              new RemoteChatSession(null, item.getPlayerKey()),
              true
          );
        }
        case LegacyPlayerListItemPacket.REMOVE_PLAYER -> {
          //Nothing should be done here, as all entries which are not allowed are removed
          // if the action is ServerUpdateTabListEvent.Action.REMOVE_PLAYER
        }
        case LegacyPlayerListItemPacket.UPDATE_DISPLAY_NAME -> {
          if (currentEntry != null) {
            currentEntry.setDisplayNameWithoutRewrite(item.getDisplayName());
          }
        }
        case LegacyPlayerListItemPacket.UPDATE_LATENCY -> {
          if (currentEntry != null) {
            currentEntry.setLatencyWithoutRewrite(item.getLatency());
          }
        }
        case LegacyPlayerListItemPacket.UPDATE_GAMEMODE -> {
          if (currentEntry != null) {
            currentEntry.setGameModeWithoutRewrite(item.getGameMode());
          }
        }
        default -> {
          // Nothing we can do here
        }
      }

      if (currentEntry != null) {
        entries.add(currentEntry);
      }
    }

    return entries;
  }

  private void processLegacy(int action, LegacyPlayerListItemPacket.Item item) {
    UUID uuid = item.getUuid();
    assert uuid != null : "1.7 tab list entry given to modern tab list handler!";

    if (action != LegacyPlayerListItemPacket.ADD_PLAYER
        && !entries.containsKey(uuid)) {
      // Sometimes UPDATE_GAMEMODE is sent before ADD_PLAYER so don't want to warn here
      return;
    }

    switch (action) {
      case LegacyPlayerListItemPacket.ADD_PLAYER -> {
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
      }
      case LegacyPlayerListItemPacket.REMOVE_PLAYER -> entries.remove(uuid);
      case LegacyPlayerListItemPacket.UPDATE_DISPLAY_NAME -> {
        KeyedVelocityTabListEntry entry = entries.get(uuid);
        if (entry != null) {
          entry.setDisplayNameInternal(item.getDisplayName());
        }
      }
      case LegacyPlayerListItemPacket.UPDATE_LATENCY -> {
        KeyedVelocityTabListEntry entry = entries.get(uuid);
        if (entry != null) {
          entry.setLatencyInternal(item.getLatency());
        }
      }
      case LegacyPlayerListItemPacket.UPDATE_GAMEMODE -> {
        KeyedVelocityTabListEntry entry = entries.get(uuid);
        if (entry != null) {
          entry.setGameModeInternal(item.getGameMode());
        }
      }
      default -> {
        // Nothing we can do here
      }
    }
  }

  void updateEntry(int action, TabListEntry entry) {
    if (entries.containsKey(entry.getProfile().getId())) {
      LegacyPlayerListItemPacket.Item packetItem = LegacyPlayerListItemPacket.Item.from(entry);

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

      connection.write(new LegacyPlayerListItemPacket(action, List.of(packetItem)));
    }
  }
}
