package com.velocitypowered.proxy.protocol;

import com.google.common.primitives.ImmutableIntArray;

public enum ProtocolConstants { ;
    public static final int LEGACY = -1;

    public static final int MINECRAFT_1_8 = 47;
    public static final int MINECRAFT_1_9 = 107;
    public static final int MINECRAFT_1_9_1 = 108;
    public static final int MINECRAFT_1_9_2 = 109;
    public static final int MINECRAFT_1_9_4 = 110;
    public static final int MINECRAFT_1_10 = 210;
    public static final int MINECRAFT_1_11 = 315;
    public static final int MINECRAFT_1_11_1 = 316;
    public static final int MINECRAFT_1_12 = 335;
    public static final int MINECRAFT_1_12_1 = 338;
    public static final int MINECRAFT_1_12_2 = 340;
    public static final int MINECRAFT_1_13 = 393;

    public static final int MINIMUM_GENERIC_VERSION = MINECRAFT_1_8;
    public static final int MAXIMUM_GENERIC_VERSION = MINECRAFT_1_13;

    public static final String SUPPORTED_GENERIC_VERSION_STRING = "1.8-1.13";

    public static final ImmutableIntArray SUPPORTED_VERSIONS = ImmutableIntArray.of(
            MINECRAFT_1_8,
            MINECRAFT_1_9,
            MINECRAFT_1_9_1,
            MINECRAFT_1_9_2,
            MINECRAFT_1_9_4,
            MINECRAFT_1_10,
            MINECRAFT_1_11,
            MINECRAFT_1_11_1,
            MINECRAFT_1_12,
            MINECRAFT_1_12_1,
            MINECRAFT_1_12_2,
            MINECRAFT_1_13
    );

    public static final String SUPPORTED_VERSIONS_STRING = "1.9-1.13";

    public static boolean isSupported(int version) {
        return SUPPORTED_VERSIONS.contains(version);
    }

    public enum Direction {
        SERVERBOUND,
        CLIENTBOUND
    }
}
