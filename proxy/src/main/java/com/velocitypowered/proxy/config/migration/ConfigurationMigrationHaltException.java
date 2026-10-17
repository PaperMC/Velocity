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

import java.io.IOException;

/**
 * Thrown once all configuration migrations have been applied, when one of them could not
 * complete on its own and the user needs to act before the proxy starts.
 */
public final class ConfigurationMigrationHaltException extends IOException {
  private static final long serialVersionUID = 1L;

  public ConfigurationMigrationHaltException(final String message) {
    super(message);
  }
}
