/*
 * Copyright (C) 2018-2026 Velocity Contributors
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
 * The old default value of 30 seconds was causing the client to time-out on itself,
 * fully disconnecting itself from the proxy instead of Velocity handling a graceful
 * transfer/disconnect.
 *
 * <p>See <a href="https://github.com/PaperMC/Velocity/issues/1819">PaperMC/Velocity#1819</a>.
 */
public final class ReadTimeoutMigration implements ConfigurationMigration {

  private static final int PREVIOUS_DEFAULT_VALUE = 30_000;
  private static final int NEW_DEFAULT_VALUE      = 25_000;

  @Override
  public boolean shouldMigrate(CommentedFileConfig config) {
    return configVersion(config) < 2.9;
  }

  @Override
  public void migrate(CommentedFileConfig config, Logger logger) {
    Integer current = config.get("advanced.read-timeout");
    if (current != null && current == PREVIOUS_DEFAULT_VALUE) {
      // Only override value if it hasn't been changed.
      config.set("advanced.read-timeout", NEW_DEFAULT_VALUE);
    }

    config.setComment("advanced.read-timeout",
        " Specify a read timeout for connections here. The default is 25 seconds.");

    config.set("config-version", "2.9");
  }
}
