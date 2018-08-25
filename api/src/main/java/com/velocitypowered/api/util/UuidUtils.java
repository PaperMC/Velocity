package com.velocitypowered.api.util;

import com.google.common.base.Preconditions;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.UUID;

/**
 * Provides a small, useful selection of utilities for working with Minecraft UUIDs.
 */
public class UuidUtils {
    private UuidUtils() {
        throw new AssertionError();
    }

    /**
     * Converts from an undashed Mojang-style UUID into a Java {@link UUID} object.
     * @param string the string to convert
     * @return the UUID object
     */
    public static @NonNull UUID fromUndashed(final @NonNull String string) {
        Objects.requireNonNull(string, "string");
        Preconditions.checkArgument(string.length() == 32, "Length is incorrect");
        return new UUID(
            Long.parseUnsignedLong(string.substring(0, 16), 16),
            Long.parseUnsignedLong(string.substring(16), 16)
        );
    }

    /**
     * Converts from a Java {@link UUID} object into an undashed Mojang-style UUID.
     * @param uuid the UUID to convert
     * @return the undashed UUID
     */
    public static @NonNull String toUndashed(final @NonNull UUID uuid) {
        Preconditions.checkNotNull(uuid, "uuid");
        return Long.toUnsignedString(uuid.getMostSignificantBits(), 16) + Long.toUnsignedString(uuid.getLeastSignificantBits(), 16);
    }

    /**
     * Generates a UUID for use for offline mode.
     * @param username the username to use
     * @return the offline mode UUID
     */
    public static @NonNull UUID generateOfflinePlayerUuid(@NonNull String username) {
        return UUID.nameUUIDFromBytes(("OfflinePlayer:" + username).getBytes(StandardCharsets.UTF_8));
    }
}
