package com.velocitypowered.proxy.util;

import com.google.common.base.Preconditions;

import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.UUID;

public enum UuidUtils {
    ;

    public static UUID fromUndashed(final String string) {
        Objects.requireNonNull(string, "string");
        Preconditions.checkArgument(string.length() == 32, "Length is incorrect");
        return new UUID(
            Long.parseUnsignedLong(string.substring(0, 16), 16),
            Long.parseUnsignedLong(string.substring(16), 16)
        );
    }

    public static String toUndashed(final UUID uuid) {
        Preconditions.checkNotNull(uuid, "uuid");
        return Long.toUnsignedString(uuid.getMostSignificantBits(), 16) + Long.toUnsignedString(uuid.getLeastSignificantBits(), 16);
    }

    public static UUID generateOfflinePlayerUuid(String username) {
        return UUID.nameUUIDFromBytes(("OfflinePlayer:" + username).getBytes(StandardCharsets.UTF_8));
    }
}
