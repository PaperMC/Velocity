package com.velocitypowered.api.util;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;

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
     * @param string the string to convert
     * @return the UUID object
     */
    public static UUID fromUndashed(final String string) {
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
    public static String toUndashed(final UUID uuid) {
        Preconditions.checkNotNull(uuid, "uuid");
        return Strings.padStart(Long.toHexString(uuid.getMostSignificantBits()), 16, '0') +
                Strings.padStart(Long.toHexString(uuid.getLeastSignificantBits()), 16, '0');
    }

    /**
     * Generates a UUID for use for offline mode.
     * @param username the username to use
     * @return the offline mode UUID
     */
    public static UUID generateOfflinePlayerUuid(String username) {
        return UUID.nameUUIDFromBytes(("OfflinePlayer:" + username).getBytes(StandardCharsets.UTF_8));
    }
}
