/*
 * Copyright (C) 2018-2021 Velocity Contributors
 *
 * The Velocity API is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package com.velocitypowered.api.permission;

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
}
