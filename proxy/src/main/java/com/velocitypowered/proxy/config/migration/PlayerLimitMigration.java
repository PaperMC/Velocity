/*
 * Copyright (C) 2026 Velocity Contributors
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
import org.apache.logging.log4j.Logger;

/**
 * Migrates the legacy player limit configuration.
 */
public final class PlayerLimitMigration implements ConfigurationMigration {

  @Override
  public boolean shouldMigrate(final CommentedFileConfig config) {
    final String version = config.getOrElse("config-version", "0");
    final String[] parts = version.split("\\.");

    final int major = Integer.parseInt(parts[0]);
    final int minor = parts.length > 1
        ? Integer.parseInt(parts[1]) : 0;

    return major < 2 || (major == 2 && minor < 10);
  }

  @Override
  public void migrate(final CommentedFileConfig config, final Logger logger) {
    if (!config.contains("player-limit")) {
      final int oldLimit = config.getIntOrElse("show-max-players", 500);
      config.set("player-limit", oldLimit);

      if (config.contains("show-max-players")) {
        logger.info("Migrated 'show-max-players' to 'player-limit' ({}).", oldLimit);
      }
    }

    config.remove("show-max-players");
    config.set("config-version", "3.0");
  }
}
