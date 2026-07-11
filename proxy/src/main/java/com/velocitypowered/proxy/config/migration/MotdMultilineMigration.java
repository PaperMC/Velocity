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
import java.util.List;
import org.apache.logging.log4j.Logger;

/**
 * Migrates a single string motd to a multiline (string list) motd.
 */
public final class MotdMultilineMigration implements ConfigurationMigration {

  @Override
  public boolean shouldMigrate(CommentedFileConfig config) {
    return configVersion(config) < 2.9;
  }

  @Override
  public void migrate(CommentedFileConfig config, Logger logger) {
    String motd = config.get("motd");
    config.set("motd", List.of(motd));

    config.set("config-version", "2.9");
  }
}
