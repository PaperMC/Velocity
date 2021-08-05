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

package com.velocitypowered.proxy.protocol.packet;

import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.proxy.connection.MinecraftSessionHandler;
import com.velocitypowered.proxy.protocol.MinecraftPacket;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import io.netty.buffer.ByteBuf;
import java.util.List;
import java.util.Set;

public class Teams implements MinecraftPacket {

  public static final byte CREATE_TEAM = 0;
  public static final byte REMOVE_TEAM = 1;
  public static final byte UPDATE_TEAM_INFO = 2;
  public static final byte ADD_ENTITIES_TO_TEAM = 3;
  public static final byte REMOVE_ENTITIES_FROM_TEAM = 4;

  private String teamName;
  private byte action;
  private String displayName;
  private byte friendlyFlags;
  private String nameTagVisibility;
  private String collisionRule;
  private int teamColor;
  private String teamPrefix;
  private String teamSuffix;
  private Set<String> entities;

  public Teams() {
  }

  public Teams(String teamName, byte action, String displayName, byte friendlyFlags, String nameTagVisibility,
               String collisionRule, int teamColor, String teamPrefix, String teamSuffix, Set<String> entities) {
    this.teamName = teamName;
    this.action = action;
    this.displayName = displayName;
    this.friendlyFlags = friendlyFlags;
    this.nameTagVisibility = nameTagVisibility;
    this.collisionRule = collisionRule;
    this.teamColor = teamColor;
    this.teamPrefix = teamPrefix;
    this.teamSuffix = teamSuffix;
    this.entities = entities;
  }

  public Teams(String teamName, byte action) {
    this.teamName = teamName;
    this.action = action;
  }

  public Teams(String teamName, byte action, String displayName, byte friendlyFlags, String nameTagVisibility,
               String collisionRule, int teamColor, String teamPrefix, String teamSuffix) {
    this.teamName = teamName;
    this.action = action;
    this.displayName = displayName;
    this.friendlyFlags = friendlyFlags;
    this.nameTagVisibility = nameTagVisibility;
    this.collisionRule = collisionRule;
    this.teamColor = teamColor;
    this.teamPrefix = teamPrefix;
    this.teamSuffix = teamSuffix;
  }

  public Teams(String teamName, byte action, Set<String> entities) {
    this.teamName = teamName;
    this.action = action;
    this.entities = entities;
  }

  @Override
  public void decode(ByteBuf buf, ProtocolUtils.Direction direction, ProtocolVersion protocolVersion) {
    throw new UnsupportedOperationException(); // encode only
  }

  @Override
  public void encode(ByteBuf buf, ProtocolUtils.Direction direction, ProtocolVersion protocolVersion) {
    if (getTeamName() == null) {
      throw new IllegalStateException("No team name found for " + getAction());
    } else if (getTeamName().length() > 16) {
      throw new IllegalStateException("Team name " + getTeamName() + " is longer than 16 characters");
    }

    ProtocolUtils.writeString(buf, getTeamName());
    buf.writeByte(getAction());

    switch (getAction()) {
      case REMOVE_TEAM:
        break;
      case CREATE_TEAM:
      case UPDATE_TEAM_INFO:
        if (getDisplayName() == null) {
          throw new IllegalStateException("No display name found for " + getAction());
        } else if (getNameTagVisibility() == null) {
          throw new IllegalStateException("No name tag visibility found for " + getAction());
        } else if (getCollisionRule() == null) {
          throw new IllegalStateException("No collision rule found for " + getAction());
        } else if (getTeamPrefix() == null) {
          throw new IllegalStateException("No team prefix found for " + getAction());
        } else if (getTeamSuffix() == null) {
          throw new IllegalStateException("No team suffix found for " + getAction());
        }

        ProtocolUtils.writeString(buf, displayName);
        buf.writeByte(friendlyFlags);
        ProtocolUtils.writeString(buf, nameTagVisibility);
        ProtocolUtils.writeString(buf, collisionRule);
        ProtocolUtils.writeVarInt(buf, teamColor);
        ProtocolUtils.writeString(buf, teamPrefix);
        ProtocolUtils.writeString(buf, teamSuffix);

        if (getAction() == UPDATE_TEAM_INFO) {
          break;
        }
      case ADD_ENTITIES_TO_TEAM:
      case REMOVE_ENTITIES_FROM_TEAM:
        if (getEntities() == null) {
          throw new IllegalStateException("No entities found for " + getAction());
        }

        ProtocolUtils.writeVarInt(buf, getEntities().size());
        for (String entity : getEntities()) {
          ProtocolUtils.writeString(buf, entity);
        }
        break;
      default:
        throw new UnsupportedOperationException("Unknown action " + getAction());
    }
  }

  public String getTeamName() {
    return teamName;
  }

  public void setTeamName(String teamName) {
    this.teamName = teamName;
  }

  public byte getAction() {
    return action;
  }

  public void setAction(byte action) {
    this.action = action;
  }

  public String getDisplayName() {
    return displayName;
  }

  public void setDisplayName(String displayName) {
    this.displayName = displayName;
  }

  public byte getFriendlyFlags() {
    return friendlyFlags;
  }

  public void setFriendlyFlags(byte friendlyFlags) {
    this.friendlyFlags = friendlyFlags;
  }

  public String getNameTagVisibility() {
    return nameTagVisibility;
  }

  public void setNameTagVisibility(String nameTagVisibility) {
    this.nameTagVisibility = nameTagVisibility;
  }

  public String getCollisionRule() {
    return collisionRule;
  }

  public void setCollisionRule(String collisionRule) {
    this.collisionRule = collisionRule;
  }

  public int getTeamColor() {
    return teamColor;
  }

  public void setTeamColor(int teamColor) {
    this.teamColor = teamColor;
  }

  public String getTeamPrefix() {
    return teamPrefix;
  }

  public void setTeamPrefix(String teamPrefix) {
    this.teamPrefix = teamPrefix;
  }

  public String getTeamSuffix() {
    return teamSuffix;
  }

  public void setTeamSuffix(String teamSuffix) {
    this.teamSuffix = teamSuffix;
  }

  public Set<String> getEntities() {
    return entities;
  }

  public void setEntities(Set<String> entities) {
    this.entities = entities;
  }

  @Override
  public boolean handle(MinecraftSessionHandler handler) {
    return handler.handle(this);
  }
}
