/*
 * Copyright (C) 2018 Velocity Contributors
 *
 * The Velocity API is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package com.velocitypowered.api.event.player;

import com.google.common.base.Preconditions;
import com.velocitypowered.api.event.annotation.AwaitingEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.player.TabListEntry;
import java.util.Set;

/**
 * This event is fired, when the tablist from velocity is updated. It can be
 * used to identify changed tablist entries or updates. Updates can only be modified.
 */
@AwaitingEvent
public class ServerUpdateTablistEvent {
  private final Player owner;

  private final Action action;

  private final Set<TabListEntry> entries;

  public ServerUpdateTablistEvent(Player owner, Action action, Set<TabListEntry> entries) {
    this.owner = Preconditions.checkNotNull(owner, "owner");
    this.action = Preconditions.checkNotNull(action, "action");
    this.entries = Preconditions.checkNotNull(entries, "entries");
  }

  public Set<TabListEntry> getEntries() {
    return entries;
  }

  public Action getAction() {
    return action;
  }

  public Player getOwner() {
    return owner;
  }

  @Override
  public String toString() {
    return "ProxyUpdateTablistEvent{"
        + "owner=" + owner
        + ", action=" + action
        + ", entries=" + entries
        + '}';
  }

  /**
   * Represents the requested action for the player list.
   */
  public enum Action {
    /**
     * Add a new player to the player list.
     */
    ADD_PLAYER(0),
    /**
     * Update the gamemode for the specific entries.
     */
    UPDATE_GAMEMODE(1),
    /**
     * Update the latency for the specific entries.
     */
    UPDATE_LATENCY(2),
    /**
     * Update the display name for the specific entries.
     */
    UPDATE_DISPLAY_NAME(3),
    /**
     * Remove the player from the player list.
     */
    REMOVE_PLAYER(4);

    private final int value;

    Action(int value) {
      this.value = value;
    }

    /**
     * Get the action from the ordinal integer code from the packet.
     *
     * @param value the id of the action
     * @return the action, null when no action could be found
     */
    public static Action of(int value) {
      for (Action action : Action.values()) {
        if (action.value == value) {
          return action;
        }
      }
      return null;
    }
  }
}
