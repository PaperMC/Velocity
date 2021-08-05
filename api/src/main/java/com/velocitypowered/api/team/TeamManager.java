/*
 * Copyright (C) 2018 Velocity Contributors
 *
 * The Velocity API is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package com.velocitypowered.api.team;

import java.util.Set;

import net.kyori.adventure.text.Component;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The manager for creating, removing and modifying {@link Team}s.
 */
public interface TeamManager {

  /**
   * This method will create a team using the provided name.
   *
   * @param name the name and identifier for the new team
   * @return the newly created team
   */
  @NotNull Team createTeam(@NotNull String name);

  /**
   * This method will create a team using the provided name and display name.
   *
   * @param name        the name and identifier for the new team
   * @param displayName the display name for the new team
   * @return the newly created team
   */
  @NotNull Team createTeam(@NotNull String name, @NotNull Component displayName);

  /**
   * This method is used for getting all of the currently existing teams.
   *
   * @return all currently existing teams
   */
  @NotNull Set<Team> getTeams();

  /**
   * This method is used for getting a team based on
   * the provided name and identifier.
   *
   * @param name the name and identifier of the team
   * @return the team or null if none found
   */
  @Nullable Team getTeam(@NotNull String name);

  /**
   * Remove the specified team.
   *
   * @param team the team to be removed
   */
  void removeTeam(@NotNull Team team);

  /**
   * Remove the specified team.
   *
   * @param team the team to be removed
   */
  void removeTeam(@NotNull String team);

  /**
   * Add an entry to the specified team.
   *
   * @param team  the team where the entry will be added
   * @param entry the entry to add
   */
  void addEntry(@NotNull Team team, @NotNull String entry);

  /**
   * Add an entry to the specified team.
   *
   * @param team  the team where the entry will be added
   * @param entry the entry to add
   */
  void addEntry(@NotNull String team, @NotNull String entry);

  /**
   * Remove an entry from the specified team.
   *
   * @param team  the team where the entry will be removed
   * @param entry the entry to remove
   */
  void removeEntry(@NotNull Team team, @NotNull String entry);

  /**
   * Remove an entry from the specified team.
   *
   * @param team  the team where the entry will be removed
   * @param entry the entry to remove
   */
  void removeEntry(@NotNull String team, @NotNull String entry);
}
