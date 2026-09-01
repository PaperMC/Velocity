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
import com.electronwill.nightconfig.core.Config;

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
  public static final PingPassthroughMode DEFAULT = new PingPassthroughMode();
  
  /**
   * Creates a default PingPassthroughMode.
   */
  private PingPassthroughMode() {
    this(false, false, false, false, false);
  }

  /**
   * Returns a PingPassthroughMode from a config section, or the default if the section is null.
   * Based on the code for PacketLimiterConfig.
   *
   * @param config The configuration object to parse.
   * @return The PingPassthroughMode, or the default if {@code config} is null.
   */
  public static PingPassthroughMode fromConfig(Config config) {
    if (config == null) {
      return DEFAULT;
    }
    return new PingPassthroughMode(
        config.getOrElse("version", DEFAULT.version()),
        config.getOrElse("players", DEFAULT.players()),
        config.getOrElse("description", DEFAULT.description()),
        config.getOrElse("favicon", DEFAULT.favicon()),
        config.getOrElse("modinfo", DEFAULT.modinfo()));
  }

  public boolean enabled() {
    return this.version || this.players || this.description || this.favicon
      || this.modinfo;
  }
}
