/*
 * Copyright (C) 2019-2024 Velocity Contributors
 *
 * The Velocity API is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package com.velocitypowered.api.event.player;

import com.google.common.base.Preconditions;
import com.velocitypowered.api.event.ResultedEvent;
import com.velocitypowered.api.event.annotation.AwaitingEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.player.TabListEntry;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * This event is fired, when a {@link com.velocitypowered.api.proxy.player.TabList Tablist} is updated
 * by a {@link com.velocitypowered.api.proxy.ServerConnection server}.
 * It can be used to override or cancel updates for {@link TabListEntry}s.
 * Velocity will wait for this event to finish firing before forwarding it to the server.
 *
 * <p>Note: If the {@code actions} contain {@link Action#REMOVE_PLAYER Action.REMOVE_PLAYER}, that may be the only action.
 *
 * <p><b>Version-specific behavior:</b>
 *   <li>For versions below 1.19.3, {@code actions} may only contain one action, and if that action
 *   is {@link Action#ADD_PLAYER Action.ADD_PLAYER}, the values normally set by other actions
 *       (e.g., {@link Action#UPDATE_GAME_MODE Action.UPDATE_GAME_MODE}) may still be set.
 *   <li>For versions below 1.8, {@code actions} may only contain {@link Action#ADD_PLAYER Action.ADD_PLAYER}
 *       or {@link Action#REMOVE_PLAYER Action.REMOVE_PLAYER}. {@link Action#ADD_PLAYER Action.ADD_PLAYER} may also act as a replacement
 *       for actions like {@link Action#UPDATE_LATENCY Action.UPDATE_LATENCY}}.
 */
@AwaitingEvent
public class ServerUpdateTabListEvent implements ResultedEvent<ServerUpdateTabListEvent.TabListUpdateResult> {

  private final Player player;
  private final Set<Action> actions;
  private final List<TabListEntry> entries;
  private TabListUpdateResult result;

  /**
   * Constructs a {@link ServerUpdateTabListEvent} instance.
   *
   * @param player the player for whom the tab list is being updated
   * @param actions the {@link Action Action}s from the server for this tab list update
   * @param entries the {@link TabListEntry}s in their updated form
   */
  public ServerUpdateTabListEvent(Player player, Set<Action> actions, List<TabListEntry> entries) {
    this.player = Preconditions.checkNotNull(player, "player");
    this.actions = Preconditions.checkNotNull(actions, "actions");
    this.entries = Preconditions.checkNotNull(entries, "entries");
    this.result = TabListUpdateResult.allowed();
  }

  public Player getPlayer() {
    return player;
  }

  public Set<Action> getActions() {
    return actions;
  }

  /**
   * The updated {@link TabListEntry}s that will be applied to the {@link com.velocitypowered.api.proxy.player.TabList Tablist}
   * of the {@code player} (or in the case of {@link Action#REMOVE_PLAYER Action.REMOVE_PLAYER} removed).
   *
   * @return the updated entries, normally immutable
   */
  public List<TabListEntry> getEntries() {
    return entries;
  }

  @Override
  public TabListUpdateResult getResult() {
    return result;
  }

  @Override
  public void setResult(TabListUpdateResult result) {
    this.result = Preconditions.checkNotNull(result, "result");
  }

  @Override
  public String toString() {
    return "ServerUpdateTabListEvent{"
        + "player=" + player
        + ", actions=" + actions
        + ", entries=" + entries
        + '}';
  }

  /**
   * Represents an action of the {@link ServerUpdateTabListEvent}.
   */
  public enum Action {
    /**
     * Add new players to the player list.
     */
    ADD_PLAYER,
    /**
     * Initialize the chat session for the entries.
     */
    INITIALIZE_CHAT,
    /**
     * Update the gamemode for the entries.
     */
    UPDATE_GAME_MODE,
    /**
     * Update the latency for the entries.
     */
    UPDATE_LISTED,
    /**
     * Update the latency for the entries.
     */
    UPDATE_LATENCY,
    /**
     * Update the display name for the specific entries.
     */
    UPDATE_DISPLAY_NAME,
    /**
     * Remove players from the player list.
     */
    REMOVE_PLAYER
  }

  /**
   * Represents the result of the {@link ServerUpdateTabListEvent}.
   */
  public static final class TabListUpdateResult implements ResultedEvent.Result {

    private static final TabListUpdateResult ALLOWED = new TabListUpdateResult(true);
    private static final TabListUpdateResult DENIED = new TabListUpdateResult(false);

    private final boolean status;
    private final Set<UUID> ids;

    public TabListUpdateResult(boolean status) {
      this.status = status;
      ids = Collections.emptySet();
    }

    public TabListUpdateResult(boolean status, Set<UUID> ids) {
      this.status = status;
      this.ids = ids;
    }

    @Override
    public boolean isAllowed() {
      return status;
    }

    public Set<UUID> getIds() {
      return ids;
    }

    /**
     * Allows the {@link TabListEntry}s to be updated, with or without modification.
     *
     * @return the allowed result
     */
    public static TabListUpdateResult allowed() {
      return ALLOWED;
    }

    /**
     * Prevents the {@link TabListEntry}s from being updated.
     *
     * @return the denied result
     */
    public static TabListUpdateResult denied() {
      return DENIED;
    }

    /**
     * Only allows specific {@link TabListEntry}s to be updated.
     * The updates for the remaining entries will be dropped.
     *
     * <p>Note: You can get the id of an entry with {@link TabListEntry#getProfile()}{@link com.velocitypowered.api.util.GameProfile#getId() .getId()}
     *
     * @param allowedOnly A non-empty set of ids of the entries that should be updated
     * @return a result with the specified entries to be updated
     */
    public static TabListUpdateResult allowedSpecific(final Set<UUID> allowedOnly) {
      Preconditions.checkNotNull(allowedOnly, "allowedOnly");
      Preconditions.checkArgument(!allowedOnly.isEmpty(), "allowedOnly empty");
      return new TabListUpdateResult(true, allowedOnly);
    }

  }

}
