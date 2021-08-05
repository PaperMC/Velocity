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

package com.velocitypowered.proxy.team;

import com.velocitypowered.api.team.Team;
import com.velocitypowered.api.team.TeamManager;
import com.velocitypowered.proxy.VelocityServer;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class VelocityTeamManager implements TeamManager {

  private final VelocityServer server;
  private final Map<String, Team> teams = new ConcurrentHashMap<>();

  /**
   * Creates a new instance.
   *
   * @param server the velocity server where this
   *               team manager is registered
   */
  public VelocityTeamManager(VelocityServer server) {
    this.server = server;
  }

  @Override
  public @NotNull Team createTeam(@NotNull String name) {
    if (teams.containsKey(name)) {
      throw new IllegalArgumentException("The team " + name + " already exists!");
    } else {
      Team team = new VelocityTeam(server, name);
      teams.put(name, team);
      return team;
    }
  }

  @Override
  public @NotNull Team createTeam(@NotNull String name, @NotNull Component displayName) {
    if (teams.containsKey(name)) {
      throw new IllegalArgumentException("The team " + name + " already exists!");
    } else {
      Team team = new VelocityTeam(server, name);
      team.setDisplayName(displayName);

      teams.put(name, team);
      return team;
    }
  }

  @Override
  public @NotNull Set<Team> getTeams() {
    return new HashSet<>(teams.values());
  }

  @Override
  public @Nullable Team getTeam(@NotNull String name) {
    return teams.get(name);
  }

  @Override
  public void removeTeam(@NotNull Team team) {
    if (teams.remove(team.getName()) != null) {
      ((VelocityTeam) team).remove();
    }
  }

  @Override
  public void removeTeam(@NotNull String team) {
    Team target = getTeam(team);

    if (target != null) {
      ((VelocityTeam) target).remove();
      teams.remove(team);
    }
  }

  @Override
  public void addEntry(@NotNull Team team, @NotNull String entry) {
    team.addEntry(entry);
  }

  @Override
  public void addEntry(@NotNull String team, @NotNull String entry) {
    Team target = getTeam(team);

    if (target != null) {
      target.addEntry(entry);
    }
  }

  @Override
  public void removeEntry(@NotNull Team team, @NotNull String entry) {
    team.removeEntry(entry);
  }

  @Override
  public void removeEntry(@NotNull String team, @NotNull String entry) {
    Team target = getTeam(team);

    if (target != null) {
      target.removeEntry(entry);
    }
  }
}
