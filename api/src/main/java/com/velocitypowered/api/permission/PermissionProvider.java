/*
 * Copyright (C) 2018-2021 Velocity Contributors
 *
 * The Velocity API is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package com.velocitypowered.api.permission;

import net.kyori.adventure.permission.PermissionChecker;

/**
 * Provides {@link PermissionChecker}s for {@link PermissionSubject}s.
 */
@FunctionalInterface
public interface PermissionProvider {

  /**
   * Creates a {@link PermissionChecker} for the subject.
   *
   * @param subject the subject
   * @return the function
   */
  PermissionChecker createChecker(PermissionSubject subject);
}
