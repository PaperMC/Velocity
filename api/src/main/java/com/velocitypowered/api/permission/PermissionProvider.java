package com.velocitypowered.api.permission;

/**
 * Provides {@link PermissionFunction}s for {@link PermissionSubject}s.
 */
@FunctionalInterface
public interface PermissionProvider {

  /**
   * Creates a {@link PermissionFunction} for the subject.
   *
   * @param subject the subject
   * @return the function
   */
  PermissionFunction createFunction(PermissionSubject subject);
}
