/*
 * Copyright (C) 2024-2026 Velocity Contributors
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

package com.velocitypowered.proxy.config.migration;

import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.velocitypowered.proxy.config.LegacyPingPassthroughMode;
import org.apache.logging.log4j.Logger;

/**
 * Migrate the old ping passthrough entry to separate config entries.
 */
public final class PingPassthroughMigration implements ConfigurationMigration {
  @Override
  public boolean shouldMigrate(final CommentedFileConfig config) {
    return configVersion(config) < 2.9;
  }

  @Override
  public void migrate(final CommentedFileConfig config, final Logger logger) {
    // Get legacy ping passthrough value
    final LegacyPingPassthroughMode legacyMode = config.getEnumOrElse("ping-passthrough",
        LegacyPingPassthroughMode.DISABLED);
    boolean version = false;
    boolean players = false;
    boolean description = false;
    boolean favicon = false;
    boolean modinfo = false;

    switch (legacyMode) {
      case ALL:
        version = true;
        players = true;
        description = true;
        favicon = true;
        modinfo = true;
        break;
      case DESCRIPTION:
        description = true;
        modinfo = true;
        break;
      case MODS:
        modinfo = true;
        break;
      case DISABLED:
        break;
      default:
        break;
    }

    config.removeComment("ping-passthrough");
    config.remove("ping-passthrough");

    config.set("ping-passthrough.version", version);
    config.setComment(
        "ping-passthrough.version",
        " Should Velocity pass the version number from the backend server when responding to server list ping requests?"
    );

    config.set("ping-passthrough.players", players);
    config.setComment(
        "ping-passthrough.players",
        " Should Velocity pass the player count from the backend server when responding to server list ping requests?"
    );

    config.set("ping-passthrough.description", description);
    config.setComment(
        "ping-passthrough.description",
        " Should Velocity pass the description from the backend server when responding to server list ping requests?"
    );

    config.set("ping-passthrough.favicon", favicon);
    config.setComment(
        "ping-passthrough.favicon",
        " Should Velocity pass the favicon (also known as the server icon) from the backend server when responding to server list ping requests?"
    );

    config.set("ping-passthrough.modinfo", modinfo);
    config.setComment(
        "ping-passthrough.modinfo",
        " Should Velocity pass the mod list from the backend server when responding to server list ping requests?"
    );

    config.set("config-version", "2.9");
  }
}
