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

import static com.velocitypowered.proxy.protocol.packet.Teams.ADD_ENTITIES_TO_TEAM;
import static com.velocitypowered.proxy.protocol.packet.Teams.CREATE_TEAM;
import static com.velocitypowered.proxy.protocol.packet.Teams.REMOVE_ENTITIES_FROM_TEAM;
import static com.velocitypowered.proxy.protocol.packet.Teams.REMOVE_TEAM;
import static com.velocitypowered.proxy.protocol.packet.Teams.UPDATE_TEAM_INFO;

import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.team.CollisionRule;
import com.velocitypowered.api.team.NameTagVisibility;
import com.velocitypowered.api.team.Team;
import com.velocitypowered.api.team.TeamColor;
import com.velocitypowered.proxy.VelocityServer;
import com.velocitypowered.proxy.connection.client.ConnectedPlayer;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import com.velocitypowered.proxy.protocol.packet.Teams;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;

public class VelocityTeam implements Team {

  private final VelocityServer server;
  private final String name;
  private final Set<String> entries;
  private Component displayName;
  private boolean allowFriendlyFire;
  private boolean canSeeFriendlyInvisibles;
  private NameTagVisibility nameTagVisibility;
  private CollisionRule collisionRule;
  private TeamColor color;
  private Component prefix;
  private Component suffix;

  /**
   * Creates a new instance.
   *
   * @param server the velocity server where the team is registered
   * @param name   the name and identifier for this team
   */
  public VelocityTeam(VelocityServer server, String name) {
    this(server, name, Component.text(name), Component.empty(), Component.empty(), TeamColor.RESET,
        true, false, NameTagVisibility.ALWAYS, CollisionRule.ALWAYS, new HashSet<>());
  }

  /**
   * Creates a new instance.
   *
   * @param server                   the velocity server where the team is registered
   * @param name                     the name and identifier for this team
   * @param displayName              the displayed name for this team
   * @param prefix                   the prefix for this team
   * @param suffix                   the suffix for this team
   * @param color                    the color for this team
   * @param allowFriendlyFire        the friendly fire status for this team
   * @param canSeeFriendlyInvisibles the friendly invisible status for this team
   * @param nameTagVisibility        the name tag visibility for this team
   * @param collisionRule            the collision rule for this team
   * @param entries                  the entries for this team
   */
  public VelocityTeam(VelocityServer server, String name, Component displayName, Component prefix, Component suffix,
                      TeamColor color, boolean allowFriendlyFire, boolean canSeeFriendlyInvisibles,
                      NameTagVisibility nameTagVisibility, CollisionRule collisionRule, Set<String> entries) {
    this.server = server;
    this.name = name;
    this.displayName = displayName;
    this.prefix = prefix;
    this.suffix = suffix;
    this.color = color;
    this.allowFriendlyFire = allowFriendlyFire;
    this.canSeeFriendlyInvisibles = canSeeFriendlyInvisibles;
    this.nameTagVisibility = nameTagVisibility;
    this.collisionRule = collisionRule;
    this.entries = entries;

    broadcast(getCreationPacket());
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public Component getDisplayName() {
    return displayName;
  }

  @Override
  public void setDisplayName(Component displayName) {
    this.displayName = displayName;
    broadcast(getUpdatePacket());
  }

  @Override
  public Component getPrefix() {
    return prefix;
  }

  @Override
  public void setPrefix(Component prefix) {
    this.prefix = prefix;
    broadcast(getUpdatePacket());
  }

  @Override
  public Component getSuffix() {
    return suffix;
  }

  @Override
  public void setSuffix(Component suffix) {
    this.suffix = suffix;
    broadcast(getUpdatePacket());
  }

  @Override
  public TeamColor getColor() {
    return color;
  }

  @Override
  public void setColor(TeamColor color) {
    this.color = color;
    broadcast(getUpdatePacket());
  }

  @Override
  public boolean isAllowFriendlyFire() {
    return allowFriendlyFire;
  }

  @Override
  public void setAllowFriendlyFire(boolean allowFriendlyFire) {
    this.allowFriendlyFire = allowFriendlyFire;
    broadcast(getUpdatePacket());
  }

  @Override
  public boolean canSeeFriendlyInvisibles() {
    return canSeeFriendlyInvisibles;
  }

  @Override
  public void setCanSeeFriendlyInvisibles(boolean canSeeFriendlyInvisibles) {
    this.canSeeFriendlyInvisibles = canSeeFriendlyInvisibles;
    broadcast(getUpdatePacket());
  }

  @Override
  public NameTagVisibility getNameTagVisibility() {
    return nameTagVisibility;
  }

  @Override
  public void setNameTagVisibility(NameTagVisibility nameTagVisibility) {
    this.nameTagVisibility = nameTagVisibility;
    broadcast(getUpdatePacket());
  }

  @Override
  public CollisionRule getCollisionRule() {
    return collisionRule;
  }

  @Override
  public void setCollisionRule(CollisionRule collisionRule) {
    this.collisionRule = collisionRule;
    broadcast(getUpdatePacket());
  }

  @Override
  public Set<String> getEntries() {
    return Collections.unmodifiableSet(entries);
  }

  @Override
  public boolean addEntry(String entry) {
    if (entries.add(entry)) {
      broadcast(getAddEntriesPacket(Collections.singleton(entry)));
      return true;
    } else {
      return false;
    }
  }

  @Override
  public boolean removeEntry(String entry) {
    if (entries.remove(entry)) {
      broadcast(getRemoveEntriesPacket(Collections.singleton(entry)));
      return true;
    } else {
      return false;
    }
  }

  @Override
  public boolean hasEntry(String entry) {
    return entries.contains(entry);
  }

  /**
   * Broadcasts the team removal packet for this team.
   */
  public void remove() {
    broadcast(getRemovalPacket());
  }

  /**
   * This method will create and return a filled {@link Teams} for this team.
   *
   * @return a filled {@link Teams} for this team
   */
  public Teams getCreationPacket() {
    GsonComponentSerializer serializer = ProtocolUtils.getJsonChatSerializer(ProtocolVersion.MAXIMUM_VERSION);

    String displayName = serializer.serialize(this.displayName);
    byte friendlyFlags = (byte) ((allowFriendlyFire ? 1 : 0) + (canSeeFriendlyInvisibles ? 2 : 0));
    String nameTagVisibility = this.nameTagVisibility.getAction();
    String collisionRule = this.collisionRule.getAction();
    int teamColor = this.color.ordinal();
    String teamPrefix = serializer.serialize(this.prefix);
    String teamSuffix = serializer.serialize(this.suffix);

    return new Teams(name, CREATE_TEAM, displayName, friendlyFlags, nameTagVisibility, collisionRule,
        teamColor, teamPrefix, teamSuffix, entries);
  }

  /**
   * This method will create and return a filled {@link Teams} for this team.
   *
   * @return a filled {@link Teams} for this team
   */
  public Teams getRemovalPacket() {
    return new Teams(name, REMOVE_TEAM);
  }

  /**
   * This method will create and return a filled {@link Teams} for this team.
   *
   * @return a filled {@link Teams} for this team
   */
  public Teams getUpdatePacket() {
    GsonComponentSerializer serializer = ProtocolUtils.getJsonChatSerializer(ProtocolVersion.MAXIMUM_VERSION);

    String displayName = serializer.serialize(this.displayName);
    byte friendlyFlags = (byte) ((allowFriendlyFire ? 1 : 0) + (canSeeFriendlyInvisibles ? 2 : 0));
    String nameTagVisibility = this.nameTagVisibility.getAction();
    String collisionRule = this.collisionRule.getAction();
    int teamColor = this.color.ordinal();
    String teamPrefix = serializer.serialize(this.prefix);
    String teamSuffix = serializer.serialize(this.suffix);

    return new Teams(name, UPDATE_TEAM_INFO, displayName, friendlyFlags, nameTagVisibility, collisionRule,
        teamColor, teamPrefix, teamSuffix);
  }

  /**
   * This method will create and return a filled {@link Teams} for this team.
   *
   * @param entries the entries in the packet
   * @return a filled {@link Teams} for this team
   */
  public Teams getAddEntriesPacket(Set<String> entries) {
    return new Teams(name, ADD_ENTITIES_TO_TEAM, entries);
  }

  /**
   * This method will create and return a filled {@link Teams} for this team.
   *
   * @param entries the entries in the packet
   * @return a filled {@link Teams} for this team
   */
  public Teams getRemoveEntriesPacket(Set<String> entries) {
    return new Teams(name, REMOVE_ENTITIES_FROM_TEAM, entries);
  }

  /**
   * A private utility method for broadcasting
   * a team packet to the entire proxy.
   *
   * @param packet the packet to broadcast
   */
  private void broadcast(Teams packet) {
    for (Player player : server.getAllPlayers()) {
      ((ConnectedPlayer) player).getConnection().write(packet);
    }
  }
}
