package com.velocitypowered.api.permission;

import org.checkerframework.checker.nullness.qual.NonNull;

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
    default boolean hasPermission(@NonNull String permission) {
        return getPermissionValue(permission).asBoolean();
    }

    /**
     * Gets the subjects setting for a particular permission.
     *
     * @param permission the permission
     * @return the value the permission is set to
     */
    @NonNull Tristate getPermissionValue(@NonNull String permission);
}
