/*
 * Copyright (C) 2018-2021 Velocity Contributors
 *
 * The Velocity API is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package com.velocitypowered.api.util;

import com.google.common.base.Preconditions;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.UUID;

/**
 * Provides a small, useful selection of utilities for working with Minecraft UUIDs.
 */
public final class UuidUtils {

  private UuidUtils() {
    throw new AssertionError();
  }

  /**
   * Converts from an undashed Mojang-style UUID into a Java {@link UUID} object.
   *
   * @param string the string to convert
   * @return the UUID object
   */
  public static UUID fromUndashed(final String string) {
    Objects.requireNonNull(string, "string");
    return FastUuidSansHyphens.parseUuid(string);
  }

  /**
   * Converts from a Java {@link UUID} object into an undashed Mojang-style UUID.
   *
   * @param uuid the UUID to convert
   * @return the undashed UUID
   */
  public static String toUndashed(final UUID uuid) {
    Preconditions.checkNotNull(uuid, "uuid");
    return FastUuidSansHyphens.toString(uuid);
  }

  /**
   * Generates a UUID for use for offline mode.
   *
   * @param username the username to use
   * @return the offline mode UUID
   */
  public static UUID generateOfflinePlayerUuid(String username) {
    return UUID.nameUUIDFromBytes(("OfflinePlayer:" + username).getBytes(StandardCharsets.UTF_8));
  }
}
