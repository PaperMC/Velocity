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
