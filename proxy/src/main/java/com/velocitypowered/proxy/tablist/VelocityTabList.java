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
import com.velocitypowered.api.proxy.player.ChatSession;
import com.velocitypowered.api.proxy.player.TabListEntry;
import com.velocitypowered.api.util.GameProfile;
import com.velocitypowered.proxy.connection.MinecraftConnection;
import com.velocitypowered.proxy.connection.client.ConnectedPlayer;
import com.velocitypowered.proxy.console.VelocityConsole;
import com.velocitypowered.proxy.protocol.packet.RemovePlayerInfo;
import com.velocitypowered.proxy.protocol.packet.UpsertPlayerInfo;
import com.velocitypowered.proxy.protocol.packet.chat.RemoteChatSession;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
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
  private final Map<UUID, VelocityTabListEntry> entries;

  /**
   * Constructs the instance.
   *
   * @param player player associated with this tab list
   */
  public VelocityTabList(ConnectedPlayer player) {
    this.player = player;
    this.connection = player.getConnection();
    this.entries = Maps.newHashMap();
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

    EnumSet<UpsertPlayerInfo.Action> actions = EnumSet.noneOf(UpsertPlayerInfo.Action.class);
    UpsertPlayerInfo.Entry playerInfoEntry = new UpsertPlayerInfo.Entry(entry.getProfile().getId());

    Preconditions.checkNotNull(entry.getProfile(), "Profile cannot be null");
    Preconditions.checkNotNull(entry.getProfile().getId(), "Profile ID cannot be null");

    TabListEntry previousEntry = this.entries.put(entry.getProfile().getId(), entry);

    if (previousEntry != null) {
      // we should merge entries here
      if (previousEntry.equals(entry)) {
        return; // nothing else to do, this entry is perfect
      }
      if (!Objects.equals(previousEntry.getDisplayNameComponent().orElse(null),
          entry.getDisplayNameComponent().orElse(null))) {
        actions.add(UpsertPlayerInfo.Action.UPDATE_DISPLAY_NAME);
        playerInfoEntry.setDisplayName(entry.getDisplayNameComponent().get());
      }
      if (!Objects.equals(previousEntry.getLatency(), entry.getLatency())) {
        actions.add(UpsertPlayerInfo.Action.UPDATE_LATENCY);
        playerInfoEntry.setLatency(entry.getLatency());
      }
      if (!Objects.equals(previousEntry.getGameMode(), entry.getGameMode())) {
        actions.add(UpsertPlayerInfo.Action.UPDATE_GAME_MODE);
        playerInfoEntry.setGameMode(entry.getGameMode());
      }
      if (!Objects.equals(previousEntry.isListed(), entry.isListed())) {
        actions.add(UpsertPlayerInfo.Action.UPDATE_LISTED);
        playerInfoEntry.setListed(entry.isListed());
      }
      if (!Objects.equals(previousEntry.getChatSession(), entry.getChatSession())) {
        ChatSession from = entry.getChatSession();
        if (from != null) {
          actions.add(UpsertPlayerInfo.Action.INITIALIZE_CHAT);
          playerInfoEntry.setChatSession(
              new RemoteChatSession(from.getSessionId(), from.getIdentifiedKey()));
        }
      }
    } else {
      actions.addAll(EnumSet.of(UpsertPlayerInfo.Action.ADD_PLAYER,
          UpsertPlayerInfo.Action.UPDATE_LATENCY,
          UpsertPlayerInfo.Action.UPDATE_LISTED));
      playerInfoEntry.setProfile(entry.getProfile());
      if (entry.getDisplayNameComponent().isPresent()) {
        actions.add(UpsertPlayerInfo.Action.UPDATE_DISPLAY_NAME);
        playerInfoEntry.setDisplayName(entry.getDisplayNameComponent().get());
      }
      if (entry.getChatSession() != null) {
        actions.add(UpsertPlayerInfo.Action.INITIALIZE_CHAT);
        ChatSession from = entry.getChatSession();
        playerInfoEntry.setChatSession(
            new RemoteChatSession(from.getSessionId(), from.getIdentifiedKey()));
      }
      if (entry.getGameMode() != -1 && entry.getGameMode() != 256) {
        actions.add(UpsertPlayerInfo.Action.UPDATE_GAME_MODE);
        playerInfoEntry.setGameMode(entry.getGameMode());
      }
      playerInfoEntry.setLatency(entry.getLatency());
      playerInfoEntry.setListed(entry.isListed());
    }
    this.connection.write(new UpsertPlayerInfo(actions, List.of(playerInfoEntry)));
  }

  @Override
  public Optional<TabListEntry> removeEntry(UUID uuid) {
    this.connection.write(new RemovePlayerInfo(List.of(uuid)));
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
    return this.entries.values().stream().map(e -> (TabListEntry) e).collect(Collectors.toList());
  }

  @Override
  public void clearAll() {
    this.connection.delayedWrite(new RemovePlayerInfo(new ArrayList<>(this.entries.keySet())));
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
  public void processUpdate(UpsertPlayerInfo infoPacket) {
    for (UpsertPlayerInfo.Entry entry : infoPacket.getEntries()) {
      processUpsert(infoPacket.getActions(), entry);
    }
  }

  protected UpsertPlayerInfo.Entry createRawEntry(VelocityTabListEntry entry) {
    Preconditions.checkNotNull(entry, "entry");
    Preconditions.checkNotNull(entry.getProfile(), "Profile cannot be null");
    Preconditions.checkNotNull(entry.getProfile().getId(), "Profile ID cannot be null");
    return new UpsertPlayerInfo.Entry(entry.getProfile().getId());
  }

  protected void emitActionRaw(UpsertPlayerInfo.Action action, UpsertPlayerInfo.Entry entry) {
    this.connection.write(
        new UpsertPlayerInfo(EnumSet.of(action), Collections.singletonList(entry)));
  }

  private void processUpsert(EnumSet<UpsertPlayerInfo.Action> actions,
      UpsertPlayerInfo.Entry entry) {
    Preconditions.checkNotNull(entry.getProfileId(), "Profile ID cannot be null");
    UUID profileId = entry.getProfileId();
    VelocityTabListEntry currentEntry = this.entries.get(profileId);
    if (actions.contains(UpsertPlayerInfo.Action.ADD_PLAYER)) {
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
    if (actions.contains(UpsertPlayerInfo.Action.UPDATE_GAME_MODE)) {
      currentEntry.setGameModeWithoutUpdate(entry.getGameMode());
    }
    if (actions.contains(UpsertPlayerInfo.Action.UPDATE_LATENCY)) {
      currentEntry.setLatencyWithoutUpdate(entry.getLatency());
    }
    if (actions.contains(UpsertPlayerInfo.Action.UPDATE_DISPLAY_NAME)) {
      currentEntry.setDisplayNameWithoutUpdate(entry.getDisplayName());
    }
    if (actions.contains(UpsertPlayerInfo.Action.INITIALIZE_CHAT)) {
      currentEntry.setChatSession(entry.getChatSession());
    }
    if (actions.contains(UpsertPlayerInfo.Action.UPDATE_LISTED)) {
      currentEntry.setListedWithoutUpdate(entry.isListed());
    }
  }

  @Override
  public void processRemove(RemovePlayerInfo infoPacket) {
    for (UUID uuid : infoPacket.getProfilesToRemove()) {
      this.entries.remove(uuid);
    }
  }
}
