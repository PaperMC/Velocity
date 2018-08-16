package com.velocitypowered.api.permission;

import org.checkerframework.checker.nullness.qual.NonNull;

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
    @NonNull PermissionFunction createFunction(@NonNull PermissionSubject subject);
}
