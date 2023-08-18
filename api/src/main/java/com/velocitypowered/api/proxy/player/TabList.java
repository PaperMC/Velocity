/*
 * Copyright (C) 2018-2022 Velocity Contributors
 *
 * The Velocity API is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package com.velocitypowered.api.proxy.player;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.crypto.IdentifiedKey;
import com.velocitypowered.api.util.GameProfile;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Represents the tab list of a {@link Player}.
 */
public interface TabList {

  /**
   * Sets the tab list header and footer for the player.
   *
   * @param header the header component
   * @param footer the footer component
   * @deprecated Use {@link Player#sendPlayerListHeaderAndFooter(Component, Component)} instead
   */
  @Deprecated
  void setHeaderAndFooter(Component header, Component footer);

  /**
   * Clears the tab list header and footer for the player.
   */
  void clearHeaderAndFooter();

  /**
   * Adds a {@link TabListEntry} to the {@link Player}'s tab list.
   *
   * @param entry to add to the tab list
   */
  void addEntry(TabListEntry entry);

  /**
   * Adds a {@link Iterable} of {@link TabListEntry}'s to the {@link Player}'s tab list.
   *
   * @param entries to add to the tab list
   */
  default void addEntries(Iterable<TabListEntry> entries) {
    for (TabListEntry entry : entries) {
      addEntry(entry);
    }
  }

  /**
   * Adds an array of {@link TabListEntry}'s to the {@link Player}'s tab list.
   *
   * @param entries to add to the tab list
   */
  default void addEntries(TabListEntry... entries) {
    for (TabListEntry entry : entries) {
      addEntry(entry);
    }
  }

  /**
   * Removes the {@link TabListEntry} from the tab list with the {@link GameProfile} identified with
   * the specified {@link UUID}.
   *
   * @param uuid of the entry
   * @return {@link Optional} containing the removed {@link TabListEntry} if present, otherwise
   *     {@link Optional#empty()}
   */
  Optional<TabListEntry> removeEntry(UUID uuid);

  /**
   * Determines if the specified entry exists in the tab list.
   *
   * @param uuid the UUID of the entry
   * @return {@code true} if it exists, {@code false} if it does not
   */
  boolean containsEntry(UUID uuid);

  /**
   * Retrieves the tab list entry associated with the given uuid.
   *
   * @param uuid The player's {@link UUID} the {@link TabListEntry} is in reference to.
   * @return {@code Optional.empty()} if the player is not present in the provided player's
   *     {@link TabList} otherwise a present {@link TabListEntry} in relation to the player.
   */
  Optional<TabListEntry> getEntry(UUID uuid);

  /**
   * Returns an immutable {@link Collection} of the {@link TabListEntry}s in the tab list.
   *
   * @return immutable {@link Collection} of tab list entries
   */
  Collection<TabListEntry> getEntries();

  /**
   * Clears all entries from the tab list.
   */
  void clearAll();

  /**
   * Builds a tab list entry.
   *
   * @param profile     profile
   * @param displayName display name
   * @param latency     latency
   * @param gameMode    game mode
   * @return entry
   * @deprecated Internal usage. Use {@link TabListEntry.Builder} instead.
   */
  @Deprecated
  default TabListEntry buildEntry(GameProfile profile, @Nullable Component displayName, int latency,
                          int gameMode) {
    return buildEntry(profile, displayName, latency, gameMode, null, true);
  }

  /**
   * Builds a tab list entry.
   *
   * @param profile     profile
   * @param displayName display name
   * @param latency     latency
   * @param gameMode    game mode
   * @param key         the player key
   * @return entry
   * @deprecated Internal usage. Use {@link TabListEntry.Builder} instead.
   */
  @Deprecated
  default TabListEntry buildEntry(GameProfile profile, @Nullable Component displayName, int latency,
                          int gameMode, @Nullable IdentifiedKey key) {
    return buildEntry(profile, displayName, latency, gameMode, null, true);
  }


  /**
   * Represents an entry in a {@link Player}'s tab list.
   *
   * @param profile     the profile
   * @param displayName the display name
   * @param latency     the latency
   * @param gameMode    the game mode
   * @param chatSession the chat session
   * @return the entry
   * @deprecated Internal usage. Use {@link TabListEntry.Builder} instead.
   */
  @Deprecated
  default TabListEntry buildEntry(GameProfile profile, @Nullable Component displayName, int latency,
                          int gameMode, @Nullable ChatSession chatSession) {
    return buildEntry(profile, displayName, latency, gameMode, chatSession, true);
  }

  /**
   * Represents an entry in a {@link Player}'s tab list.
   *
   * @param profile     the profile
   * @param displayName the display name
   * @param latency     the latency
   * @param gameMode    the game mode
   * @param chatSession the chat session
   * @param listed      the visible status of entry
   * @return the entry
   * @deprecated Internal usage. Use {@link TabListEntry.Builder} instead.
   */
  @Deprecated
  TabListEntry buildEntry(GameProfile profile, @Nullable Component displayName, int latency,
                          int gameMode, @Nullable ChatSession chatSession, boolean listed);
}
