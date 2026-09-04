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

import com.velocitypowered.proxy.config.YamlDocument;
import java.io.IOException;
import org.apache.logging.log4j.Logger;

/**
 * Migration applied to a YAML configuration. Versions are whole numbers starting at 3, the
 * version the TOML format was converted into.
 */
public interface ConfigurationMigration {

  boolean shouldMigrate(YamlDocument config);

  void migrate(YamlDocument config, Logger logger) throws IOException;

  /**
   * Gets the configuration version, which every configuration must declare.
   */
  static int versionOf(final YamlDocument config) {
    final Object version = config.get("config-version");
    if (version instanceof Number number) {
      return number.intValue();
    }
    throw new IllegalStateException(version == null
        ? "Your configuration does not declare a config-version."
        : "Your configuration declares a config-version of '" + version
            + "', which is not a whole number.");
  }

  default int configVersion(final YamlDocument config) {
    return versionOf(config);
  }
}
