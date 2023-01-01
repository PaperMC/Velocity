/*
 * Copyright (C) 2018-2022 Velocity Contributors
 *
 * The Velocity API is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package com.velocitypowered.api.permission;

import net.kyori.adventure.permission.PermissionChecker;

/**
 * Represents a object that has a set of queryable permissions.
 */
public interface PermissionSubject {

  /**
   * Determines whether or not the subject has a particular permission.
   *
   * @param permission the permission to check for
   * @return whether or not the subject has the permission
   */
  default boolean hasPermission(String permission) {
    return getPermissionValue(permission).asBoolean();
  }

  /**
   * Gets the subjects setting for a particular permission.
   *
   * @param permission the permission
   * @return the value the permission is set to
   */
  Tristate getPermissionValue(String permission);

  /**
   * Gets the permission checker for the subject.
   *
   * @return subject's permission checker
   */
  default PermissionChecker getPermissionChecker() {
    return permission -> getPermissionValue(permission).toAdventureTriState();
  }
}
