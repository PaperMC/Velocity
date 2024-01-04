/*
 * Copyright (C) 2023 Velocity Contributors
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
import java.io.IOException;
import org.apache.logging.log4j.Logger;

/**
 * Configuration Migration interface.
 */
public sealed interface ConfigurationMigration
        permits ForwardingMigration, KeyAuthenticationMigration, MotdMigration {
  boolean shouldMigrate(CommentedFileConfig config);

  void migrate(CommentedFileConfig config, Logger logger) throws IOException;

  /**
   * Gets the configuration version.
   *
   * @param config the configuration.
   * @return configuration version
   */
  default double configVersion(CommentedFileConfig config) {
    final String stringVersion = config.getOrElse("config-version", "1.0");
    try {
      return Double.parseDouble(stringVersion);
    } catch (Exception e) {
      return 1.0;
    }
  }
}
