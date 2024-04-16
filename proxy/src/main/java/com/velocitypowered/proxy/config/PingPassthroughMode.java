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

/**
 * Object to contain all of the things that can be toggled for ping passthrough.
 */
public class PingPassthroughMode {
  public boolean version;
  public boolean players;
  public boolean description;
  public boolean favicon;
  public boolean modinfo;

  /**
   * Passthrough mode constructor.
   * Looking at other code, I'm not sure the constructor is supposed to need a javadoc style comment,
   * but checkstyle was yelling at me because I didn't include one.
   * Probably for the best.
   *
   * @param version whether the version should be passed through.
   * @param players whether the player count should be passed through.
   * @param description whether the description should be passed through.
   * @param favicon whether the favicon should be passed through.
   * @param modinfo whether the modinfo should be passed through.
   */
  public PingPassthroughMode(boolean version, boolean players, boolean description, boolean favicon, boolean modinfo) {
    this.version = version;
    this.players = players;
    this.description = description;
    this.favicon = favicon;
    this.modinfo = modinfo;
  }

  public boolean enabled() {
    return this.version || this.players || this.description || this.favicon || this.modinfo;
  }
}
