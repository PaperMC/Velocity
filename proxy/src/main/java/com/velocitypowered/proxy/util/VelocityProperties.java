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

package com.velocitypowered.proxy.util;

import static java.util.Objects.requireNonNull;

/**
 * Utils for easy handling of properties.
 *
 * @since 3.3.0
 */
public final class VelocityProperties {
  /**
   * Attempts to read a system property as boolean.
   *
   * @param property the system property to read
   * @param defaultValue the default value
   * @return the default value if the property is not set,
   *     if the string is present and is "true" (case-insensitive),
   *     it will return {@code true}, otherwise, it will return false.
   * @since 3.3.0
   */
  public static boolean readBoolean(final String property, final boolean defaultValue) {
    requireNonNull(property);
    final String value = System.getProperty(property);
    if (value == null) {
      return defaultValue;
    }
    return Boolean.parseBoolean(value);
  }

  /**
   * Check if there is a value assigned to this system property.
   *
   * @param property the system property to check
   * @return if a value is assigned to this system property
   * @since 3.3.0
   */
  public static boolean hasProperty(final String property) {
    requireNonNull(property);

    return System.getProperty(property) != null;
  }
}
