/*
 * Copyright (C) 2024 Velocity Contributors
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
 * Creation of the configuration option "accepts-transfers".
 */
public final class TransferIntegrationMigration implements ConfigurationMigration {
  @Override
  public boolean shouldMigrate(final CommentedFileConfig config) {
    return configVersion(config) < 2.7;
  }

  @Override
  public void migrate(final CommentedFileConfig config, final Logger logger) {
    config.set("advanced.accepts-transfers", false);
    config.setComment("advanced.accepts-transfers", """
            Allows players transferred from other hosts via the
            Transfer packet (Minecraft 1.20.5) to be received.""");
    config.set("config-version", "2.7");
  }
}
