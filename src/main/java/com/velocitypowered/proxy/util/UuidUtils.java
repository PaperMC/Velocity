package com.velocitypowered.proxy.util;

import com.google.common.base.Preconditions;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

public enum UuidUtils {
    ;

    public static UUID fromMojang(String id) {
        Preconditions.checkNotNull(id, "id");
        Preconditions.checkArgument(id.length() == 32, "Length is incorrect");
        return UUID.fromString(
                id.substring(0, 8) + "-" +
                        id.substring(8, 12) + "-" +
                        id.substring(12, 16) + "-" +
                        id.substring(16, 20) + "-" +
                        id.substring(20, 32)
        );
    }

    public static UUID generateOfflinePlayerUuid(String username) {
        return UUID.nameUUIDFromBytes(("OfflinePlayer:" + username).getBytes(StandardCharsets.UTF_8));
    }
}
