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
 * Creation of the configuration option "forced-hosts.subdomain-matching".
 */
public final class ForcedHostSubdomainMigration implements ConfigurationMigration {
  @Override
  public boolean shouldMigrate(final CommentedFileConfig config) {
    return configVersion(config) < 2.8;
  }

  @Override
  public void migrate(final CommentedFileConfig config, final Logger logger) {
    config.set("forced-hosts.subdomain-matching", false);
    config.setComment("forced-hosts.subdomain-matching", """
            When enabled, if no exact forced host match is found, Velocity will check if
            the hostname is a subdomain of any configured forced host (on a dot boundary).
            Useful when DNS services prepend prefixes to hostnames.""");
    config.set("config-version", "2.8");
  }
}
