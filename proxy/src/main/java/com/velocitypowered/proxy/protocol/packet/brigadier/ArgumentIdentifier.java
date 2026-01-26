/*
 * Copyright (C) 2022-2023 Velocity Contributors
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

package com.velocitypowered.proxy.protocol.packet.brigadier;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.velocitypowered.api.network.ProtocolVersion;
import java.util.HashMap;
import java.util.Map;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Represents an identifier for a Brigadier command argument, mapping the argument to
 * different protocol versions.
 *
 * <p>The {@code ArgumentIdentifier} is responsible for holding an identifier string for
 * an argument and a map that associates protocol versions with their respective IDs.
 * It ensures that the protocol version is compatible with the Minecraft 1.19 protocol or later.</p>
 */
public class ArgumentIdentifier {

  private final String identifier;
  private final Map<ProtocolVersion, Integer> versionById;

  private ArgumentIdentifier(String identifier, VersionSet... versions) {
    this.identifier = Preconditions.checkNotNull(identifier);

    Preconditions.checkNotNull(versions);

    Map<ProtocolVersion, Integer> temp = new HashMap<>();

    ProtocolVersion previous = null;
    for (VersionSet version : versions) {
      VersionSet current = Preconditions.checkNotNull(version);

      Preconditions.checkArgument(
          current.getVersion().noLessThan(ProtocolVersion.MINECRAFT_1_19),
          "Version too old for ID index");
      Preconditions.checkArgument(previous == null || previous.greaterThan(current.getVersion()),
          "Invalid protocol version order");

      for (ProtocolVersion v : ProtocolVersion.values()) {
        if (v.noLessThan(current.getVersion())) {
          temp.putIfAbsent(v, current.getId());
        }
      }
      previous = current.getVersion();

    }

    this.versionById = ImmutableMap.copyOf(temp);
  }

  @Override
  public String toString() {
    return "ArgumentIdentifier{"
        + "identifier='" + identifier + '\''
        + '}';
  }

  public String getIdentifier() {
    return identifier;
  }

  public @Nullable Integer getIdByProtocolVersion(ProtocolVersion version) {
    return versionById.get(Preconditions.checkNotNull(version));
  }

  public static VersionSet mapSet(ProtocolVersion version, int id) {
    return new VersionSet(version, id);
  }

  public static ArgumentIdentifier id(String identifier, VersionSet... versions) {
    return new ArgumentIdentifier(identifier, versions);
  }

  /**
   * This class is purely for convenience.
   */
  public static class VersionSet {

    private final ProtocolVersion version;
    private final int id;

    private VersionSet(ProtocolVersion version, int id) {
      this.version = Preconditions.checkNotNull(version);
      this.id = id;
    }

    public int getId() {
      return id;
    }

    public ProtocolVersion getVersion() {
      return version;
    }
  }

}
