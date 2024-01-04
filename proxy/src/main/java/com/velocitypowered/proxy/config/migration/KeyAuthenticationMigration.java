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
import org.apache.logging.log4j.Logger;

/**
 * Creation of the configuration option "force-key-authentication".
 */
public final class KeyAuthenticationMigration implements ConfigurationMigration {
  @Override
  public boolean shouldMigrate(final CommentedFileConfig config) {
    final double version = configVersion(config);
    return version == 1.0 || version == 2.0;
  }

  @Override
  public void migrate(final CommentedFileConfig config, final Logger logger) {
    config.set("force-key-authentication", config.getOrElse("force-key-authentication", true));
    config.setComment("force-key-authentication",
            "Should the proxy enforce the new public key security standard? By default,"
                    + " this is on.");
    config.set("config-version", configVersion(config) == 2.0 ? "2.5" : "1.5");
  }
}
