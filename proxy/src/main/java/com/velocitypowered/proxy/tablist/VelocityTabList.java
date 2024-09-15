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
import com.google.common.collect.Maps;
import com.velocitypowered.api.event.player.ServerUpdateTabListEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.player.ChatSession;
import com.velocitypowered.api.proxy.player.TabListEntry;
import com.velocitypowered.api.util.GameProfile;
import com.velocitypowered.proxy.connection.MinecraftConnection;
import com.velocitypowered.proxy.connection.client.ConnectedPlayer;
import com.velocitypowered.proxy.console.VelocityConsole;
import com.velocitypowered.proxy.protocol.packet.RemovePlayerInfoPacket;
import com.velocitypowered.proxy.protocol.packet.UpsertPlayerInfoPacket;
import com.velocitypowered.proxy.protocol.packet.chat.ComponentHolder;
import com.velocitypowered.proxy.protocol.packet.chat.RemoteChatSession;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Base class for handling tab lists.
 */
public class VelocityTabList implements InternalTabList {

  private static final Logger logger = LogManager.getLogger(VelocityConsole.class);
  private final ConnectedPlayer player;
  private final MinecraftConnection connection;
  protected final ProxyServer proxyServer;
  private final Map<UUID, VelocityTabListEntry> entries;

  /**
   * Constructs the instance.
   *
   * @param player player associated with this tab list
   */
  public VelocityTabList(ConnectedPlayer player, final ProxyServer proxyServer) {
    this.player = player;
    this.proxyServer = proxyServer;
    this.connection = player.getConnection();
    this.entries = Maps.newConcurrentMap();
  }

  @Override
  public Player getPlayer() {
    return player;
  }

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
    VelocityTabListEntry entry;
    if (entry1 instanceof VelocityTabListEntry) {
      entry = (VelocityTabListEntry) entry1;
    } else {
      entry = new VelocityTabListEntry(this, entry1.getProfile(),
          entry1.getDisplayNameComponent().orElse(null),
          entry1.getLatency(), entry1.getGameMode(), entry1.getChatSession(), entry1.isListed());
    }

    EnumSet<UpsertPlayerInfoPacket.Action> actions = EnumSet
            .noneOf(UpsertPlayerInfoPacket.Action.class);
    UpsertPlayerInfoPacket.Entry playerInfoEntry = new UpsertPlayerInfoPacket
            .Entry(entry.getProfile().getId());

    Preconditions.checkNotNull(entry.getProfile(), "Profile cannot be null");
    Preconditions.checkNotNull(entry.getProfile().getId(), "Profile ID cannot be null");

    this.entries.compute(entry.getProfile().getId(), (uuid, previousEntry) -> {
      if (previousEntry != null) {
        // we should merge entries here
        if (previousEntry.equals(entry)) {
          return previousEntry; // nothing else to do, this entry is perfect
        }
        if (!Objects.equals(previousEntry.getDisplayNameComponent().orElse(null),
                entry.getDisplayNameComponent().orElse(null))) {
          actions.add(UpsertPlayerInfoPacket.Action.UPDATE_DISPLAY_NAME);
          playerInfoEntry.setDisplayName(entry.getDisplayNameComponent().isEmpty()
                  ?
                  null :
                  new ComponentHolder(player.getProtocolVersion(),
                          entry.getDisplayNameComponent().get())
          );
        }
        if (!Objects.equals(previousEntry.getLatency(), entry.getLatency())) {
          actions.add(UpsertPlayerInfoPacket.Action.UPDATE_LATENCY);
          playerInfoEntry.setLatency(entry.getLatency());
        }
        if (!Objects.equals(previousEntry.getGameMode(), entry.getGameMode())) {
          actions.add(UpsertPlayerInfoPacket.Action.UPDATE_GAME_MODE);
          playerInfoEntry.setGameMode(entry.getGameMode());
        }
        if (!Objects.equals(previousEntry.isListed(), entry.isListed())) {
          actions.add(UpsertPlayerInfoPacket.Action.UPDATE_LISTED);
          playerInfoEntry.setListed(entry.isListed());
        }
        if (!Objects.equals(previousEntry.getChatSession(), entry.getChatSession())) {
          ChatSession from = entry.getChatSession();
          if (from != null) {
            actions.add(UpsertPlayerInfoPacket.Action.INITIALIZE_CHAT);
            playerInfoEntry.setChatSession(
                    new RemoteChatSession(from.getSessionId(), from.getIdentifiedKey()));
          }
        }
      } else {
        actions.addAll(EnumSet.of(UpsertPlayerInfoPacket.Action.ADD_PLAYER,
                UpsertPlayerInfoPacket.Action.UPDATE_LATENCY,
                UpsertPlayerInfoPacket.Action.UPDATE_LISTED));
        playerInfoEntry.setProfile(entry.getProfile());
        if (entry.getDisplayNameComponent().isPresent()) {
          actions.add(UpsertPlayerInfoPacket.Action.UPDATE_DISPLAY_NAME);
          playerInfoEntry.setDisplayName(entry.getDisplayNameComponent().isEmpty()
                  ?
                  null :
                  new ComponentHolder(player.getProtocolVersion(),
                          entry.getDisplayNameComponent().get())
          );
        }
        if (entry.getChatSession() != null) {
          actions.add(UpsertPlayerInfoPacket.Action.INITIALIZE_CHAT);
          ChatSession from = entry.getChatSession();
          playerInfoEntry.setChatSession(
                  new RemoteChatSession(from.getSessionId(), from.getIdentifiedKey()));
        }
        if (entry.getGameMode() != -1 && entry.getGameMode() != 256) {
          actions.add(UpsertPlayerInfoPacket.Action.UPDATE_GAME_MODE);
          playerInfoEntry.setGameMode(entry.getGameMode());
        }
        playerInfoEntry.setLatency(entry.getLatency());
        playerInfoEntry.setListed(entry.isListed());
      }
      return entry;
    });

    if (!actions.isEmpty()) {
      this.connection.write(new UpsertPlayerInfoPacket(actions, List.of(playerInfoEntry)));
    }
  }

  @Override
  public Optional<TabListEntry> removeEntry(UUID uuid) {
    this.connection.write(new RemovePlayerInfoPacket(List.of(uuid)));
    return Optional.ofNullable(this.entries.remove(uuid));
  }

  @Override
  public boolean containsEntry(UUID uuid) {
    return this.entries.containsKey(uuid);
  }

  @Override
  public Optional<TabListEntry> getEntry(UUID uuid) {
    return Optional.ofNullable(this.entries.get(uuid));
  }

  @Override
  public Collection<TabListEntry> getEntries() {
    return List.copyOf(this.entries.values());
  }

  @Override
  public void clearAll() {
    this.connection.delayedWrite(new RemovePlayerInfoPacket(
            new ArrayList<>(this.entries.keySet())));
    clearAllSilent();
  }

  @Override
  public void clearAllSilent() {
    this.entries.clear();
  }

  @Override
  public TabListEntry buildEntry(GameProfile profile, @Nullable Component displayName, int latency,
      int gameMode,
      @Nullable ChatSession chatSession, boolean listed) {
    return new VelocityTabListEntry(this, profile, displayName, latency, gameMode, chatSession,
        listed);
  }

  @Override
  public void processUpdate(UpsertPlayerInfoPacket infoPacket) {
    List<UpdateEventTabListEntry> entries = mapToEventEntries(infoPacket.getActions(), infoPacket.getEntries());

    proxyServer.getEventManager().fire(new ServerUpdateTabListEvent(player,
        Collections.unmodifiableSet(mapToEventActions(infoPacket.getActions())),
        Collections.unmodifiableList(entries))
    ).thenAcceptAsync(event -> {
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
            for (UpdateEventTabListEntry entry : entries) {
              addEntry(entry);
            }
          } else {
            //listeners haven't modified entries
            for (UpsertPlayerInfoPacket.Entry entry : infoPacket.getEntries()) {
              processUpsert(infoPacket.getActions(), entry);
            }

            connection.write(infoPacket);
          }
        } else {
          //listeners have denied entries (and may have modified others), requires manual processing
          for (UpdateEventTabListEntry entry : entries) {
            if (event.getResult().getIds().contains(entry.getProfile().getId())) {
              addEntry(entry);
            }
          }
        }
      }
    });
  }

  protected UpsertPlayerInfoPacket.Entry createRawEntry(VelocityTabListEntry entry) {
    Preconditions.checkNotNull(entry, "entry");
    Preconditions.checkNotNull(entry.getProfile(), "Profile cannot be null");
    Preconditions.checkNotNull(entry.getProfile().getId(), "Profile ID cannot be null");
    return new UpsertPlayerInfoPacket.Entry(entry.getProfile().getId());
  }

  protected void emitActionRaw(UpsertPlayerInfoPacket.Action action,
                               UpsertPlayerInfoPacket.Entry entry) {
    this.connection.write(new UpsertPlayerInfoPacket(EnumSet.of(action), List.of(entry)));
  }

  private EnumSet<ServerUpdateTabListEvent.Action> mapToEventActions(EnumSet<UpsertPlayerInfoPacket.Action> packetActions) {
    EnumSet<ServerUpdateTabListEvent.Action> actions = EnumSet.noneOf(ServerUpdateTabListEvent.Action.class);

    for (UpsertPlayerInfoPacket.Action packetAction : packetActions) {
      switch (packetAction) {
        case ADD_PLAYER -> {
          actions.add(ServerUpdateTabListEvent.Action.ADD_PLAYER);
        }
        case INITIALIZE_CHAT -> {
          actions.add(ServerUpdateTabListEvent.Action.INITIALIZE_CHAT);
        }
        case UPDATE_GAME_MODE -> {
          actions.add(ServerUpdateTabListEvent.Action.UPDATE_GAME_MODE);
        }
        case UPDATE_LISTED -> {
          actions.add(ServerUpdateTabListEvent.Action.UPDATE_LISTED);
        }
        case UPDATE_LATENCY -> {
          actions.add(ServerUpdateTabListEvent.Action.UPDATE_LATENCY);
        }
        case UPDATE_DISPLAY_NAME -> {
          actions.add(ServerUpdateTabListEvent.Action.UPDATE_DISPLAY_NAME);
        }
        default -> {
          // Nothing we can do here
        }
      }
    }

    return actions;
  }

  private List<UpdateEventTabListEntry> mapToEventEntries(EnumSet<UpsertPlayerInfoPacket.Action> actions,
                                                          List<UpsertPlayerInfoPacket.Entry> packetEntries) {
    List<UpdateEventTabListEntry> entries = new ArrayList<>(packetEntries.size());

    for (UpsertPlayerInfoPacket.Entry rawEntry : packetEntries) {
      Preconditions.checkNotNull(rawEntry.getProfileId(), "Profile ID cannot be null");
      UUID profileId = rawEntry.getProfileId();
      UpdateEventTabListEntry currentEntry = null;
      VelocityTabListEntry oldCurrentEntry = this.entries.get(profileId);

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

      if (actions.contains(UpsertPlayerInfoPacket.Action.ADD_PLAYER)) {
        if (currentEntry == null) {
          currentEntry = new UpdateEventTabListEntry(
              this,
              rawEntry.getProfile(),
              null,
              0,
              -1,
              null,
              false
          );
        } else {
          logger.debug("Received an add player packet for an existing entry; this does nothing.");
        }
      } else if (currentEntry == null) {
        logger.debug(
            "Received a partial player before an ADD_PLAYER action; profile could not be built. {}",
            rawEntry);
        continue;
      } else {
        currentEntry = new UpdateEventTabListEntry(
            this,
            currentEntry.getProfile(),
            currentEntry.getDisplayNameComponent().orElse(null),
            currentEntry.getLatency(),
            currentEntry.getGameMode(),
            currentEntry.getChatSession(),
            currentEntry.isListed()
        );
      }
      if (actions.contains(UpsertPlayerInfoPacket.Action.UPDATE_GAME_MODE)) {
        currentEntry.setGameModeWithoutRewrite(rawEntry.getGameMode());
      }
      if (actions.contains(UpsertPlayerInfoPacket.Action.UPDATE_LATENCY)) {
        currentEntry.setLatencyWithoutRewrite(rawEntry.getLatency());
      }
      if (actions.contains(UpsertPlayerInfoPacket.Action.UPDATE_DISPLAY_NAME)) {
        currentEntry.setDisplayNameWithoutRewrite(rawEntry.getDisplayName() != null
            ? rawEntry.getDisplayName().getComponent() : null);
      }
      if (actions.contains(UpsertPlayerInfoPacket.Action.INITIALIZE_CHAT)) {
        currentEntry.setChatSessionWithoutRewrite(rawEntry.getChatSession());
      }
      if (actions.contains(UpsertPlayerInfoPacket.Action.UPDATE_LISTED)) {
        currentEntry.setListedWithoutRewrite(rawEntry.isListed());
      }
      entries.add(currentEntry);
    }

    return entries;
  }

  private List<UpdateEventTabListEntry> mapToEventEntries(Collection<UUID> uuids) {
    List<UpdateEventTabListEntry> entries = new ArrayList<>();

    for (Map.Entry<UUID, VelocityTabListEntry> entry : this.entries.entrySet()) {
      if (uuids.contains(entry.getKey())) {
        entries.add(
            new UpdateEventTabListEntry(
                this,
                entry.getValue().getProfile(),
                entry.getValue().getDisplayNameComponent().orElse(null),
                entry.getValue().getLatency(),
                entry.getValue().getGameMode(),
                entry.getValue().getChatSession(),
                entry.getValue().isListed()
            )
        );
      }
    }

    return entries;
  }

  private void processUpsert(EnumSet<UpsertPlayerInfoPacket.Action> actions,
      UpsertPlayerInfoPacket.Entry entry) {
    Preconditions.checkNotNull(entry.getProfileId(), "Profile ID cannot be null");
    UUID profileId = entry.getProfileId();
    VelocityTabListEntry currentEntry = this.entries.get(profileId);
    if (actions.contains(UpsertPlayerInfoPacket.Action.ADD_PLAYER)) {
      if (currentEntry == null) {
        this.entries.put(profileId,
            currentEntry = new VelocityTabListEntry(
                this,
                entry.getProfile(),
                null,
                0,
                -1,
                null,
                false
            )
        );
      } else {
        logger.debug("Received an add player packet for an existing entry; this does nothing.");
      }
    } else if (currentEntry == null) {
      logger.debug(
          "Received a partial player before an ADD_PLAYER action; profile could not be built. {}",
          entry);
      return;
    }
    if (actions.contains(UpsertPlayerInfoPacket.Action.UPDATE_GAME_MODE)) {
      currentEntry.setGameModeWithoutUpdate(entry.getGameMode());
    }
    if (actions.contains(UpsertPlayerInfoPacket.Action.UPDATE_LATENCY)) {
      currentEntry.setLatencyWithoutUpdate(entry.getLatency());
    }
    if (actions.contains(UpsertPlayerInfoPacket.Action.UPDATE_DISPLAY_NAME)) {
      currentEntry.setDisplayNameWithoutUpdate(entry.getDisplayName() != null
          ? entry.getDisplayName().getComponent() : null);
    }
    if (actions.contains(UpsertPlayerInfoPacket.Action.INITIALIZE_CHAT)) {
      currentEntry.setChatSession(entry.getChatSession());
    }
    if (actions.contains(UpsertPlayerInfoPacket.Action.UPDATE_LISTED)) {
      currentEntry.setListedWithoutUpdate(entry.isListed());
    }
  }

  @Override
  public void processRemove(RemovePlayerInfoPacket infoPacket) {
    List<UpdateEventTabListEntry> entries = mapToEventEntries(infoPacket.getProfilesToRemove());

    proxyServer.getEventManager().fire(
        new ServerUpdateTabListEvent(
            player,
            Set.of(ServerUpdateTabListEvent.Action.REMOVE_PLAYER),
            Collections.unmodifiableList(entries)
        )
    ).thenAcceptAsync(event -> { //not sure what should be used here!
      if (event.getResult().isAllowed()) {
        if (event.getResult().getIds().isEmpty()) {
          for (UUID uuid : infoPacket.getProfilesToRemove()) {
            this.entries.remove(uuid);
          }

          connection.write(infoPacket);
        } else {
          List<UUID> uuids = new ArrayList<>();
          for (UUID uuid : infoPacket.getProfilesToRemove()) {
            if (event.getResult().getIds().contains(uuid)) {
              this.entries.remove(uuid);
              uuids.add(uuid);
            }
          }

          this.connection.write(new RemovePlayerInfoPacket(uuids));
        }
      }
    });
  }
}
