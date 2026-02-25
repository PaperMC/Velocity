/*
 * Copyright (C) 2018-2021 Velocity Contributors
 *
 * The Velocity API is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package com.velocitypowered.api.permission;

import java.util.Map;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Function that calculates the permission settings for a given {@link PermissionSubject}.
 */
@FunctionalInterface
public interface PermissionFunction {

  /**
   * A permission function that always returns {@link Tristate#TRUE}.
   */
  PermissionFunction ALWAYS_TRUE = p -> Tristate.TRUE;

  /**
   * A permission function that always returns {@link Tristate#FALSE}.
   */
  PermissionFunction ALWAYS_FALSE = p -> Tristate.FALSE;

  /**
   * A permission function that always returns {@link Tristate#UNDEFINED}.
   */
  PermissionFunction ALWAYS_UNDEFINED = p -> Tristate.UNDEFINED;

  /**
   * Gets the subjects setting for a particular permission.
   *
   * @param permission the permission
   * @return the value the permission is set to
   */
  Tristate getPermissionValue(String permission);

  /**
   * Gets the subjects permission map for any set permission.
   * There does not have to be a guarantee that when {@link #getPermissionValue} returns {@link Tristate#TRUE} or {@link Tristate#FALSE}
   * for a given permission, that it should also be contained within this permission map.
   *
   * @return the permission map, or {@code null} if the implementing provider does not expose this information.
   */
  default @Nullable Map<String, Boolean> getPermissionMap() {
    return null;
  }
}
