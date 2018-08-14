package com.velocitypowered.api.util;

import com.google.common.base.Preconditions;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.UUID;

public class UuidUtils {
    private UuidUtils() {
        throw new AssertionError();
    }

    public static @NonNull UUID fromUndashed(final @NonNull String string) {
        Objects.requireNonNull(string, "string");
        Preconditions.checkArgument(string.length() == 32, "Length is incorrect");
        return new UUID(
            Long.parseUnsignedLong(string.substring(0, 16), 16),
            Long.parseUnsignedLong(string.substring(16), 16)
        );
    }

    public static @NonNull String toUndashed(final @NonNull UUID uuid) {
        Preconditions.checkNotNull(uuid, "uuid");
        return Long.toUnsignedString(uuid.getMostSignificantBits(), 16) + Long.toUnsignedString(uuid.getLeastSignificantBits(), 16);
    }

    public static @NonNull UUID generateOfflinePlayerUuid(@NonNull String username) {
        return UUID.nameUUIDFromBytes(("OfflinePlayer:" + username).getBytes(StandardCharsets.UTF_8));
    }
}
