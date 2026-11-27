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

package com.velocitypowered.proxy.config;

import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;

/**
 * Object to contain all the things that can be toggled for ping passthrough.
 *
 * @param version     Whether the version should be passed through.
 * @param players     Whether the player count should be passed through.
 * @param description Whether the description should be passed through.
 * @param favicon     Whether the favicon should be passed through.
 * @param modinfo     Whether the modinfo should be passed through.
 */
public record PingPassthroughMode(boolean version, boolean players,
    boolean description, boolean favicon, boolean modinfo) {

  /**
   * Returns the PingPassthroughMode in a config section.
   *
   * @param section The configuration section to parse.
   * @return The PingPassthroughMode.
   */
  public static PingPassthroughMode fromConfig(ConfigurationNode section)
      throws SerializationException {
    return new PingPassthroughMode(
        section.node("version").require(Boolean.class),
        section.node("players").require(Boolean.class),
        section.node("description").require(Boolean.class),
        section.node("favicon").require(Boolean.class),
        section.node("modinfo").require(Boolean.class));
  }

  public boolean enabled() {
    return this.version || this.players || this.description || this.favicon
        || this.modinfo;
  }
}
