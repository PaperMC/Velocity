/*
 * Copyright (C) 2018 Velocity Contributors
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
  TabListEntry buildEntry(GameProfile profile, @Nullable Component displayName, int latency,
                          int gameMode);

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
  TabListEntry buildEntry(GameProfile profile, @Nullable Component displayName, int latency,
                          int gameMode, @Nullable IdentifiedKey key);


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
  TabListEntry buildEntry(GameProfile profile, @Nullable Component displayName, int latency,
                          int gameMode, @Nullable ChatSession chatSession);
}
