package com.velocitypowered.proxy.protocol;

import java.util.Arrays;

public enum ProtocolConstants { ;
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

    public static final int MINIMUM_GENERIC_VERSION = MINECRAFT_1_9;

    public static final int[] SUPPORTED_VERSIONS = new int[] {
            MINECRAFT_1_9,
            MINECRAFT_1_9_1,
            MINECRAFT_1_9_2,
            MINECRAFT_1_9_4,
            MINECRAFT_1_10,
            MINECRAFT_1_11,
            MINECRAFT_1_11_1,
            MINECRAFT_1_12,
            MINECRAFT_1_12_1,
            MINECRAFT_1_12_2
    };

    public static boolean isSupported(int version) {
        return Arrays.binarySearch(SUPPORTED_VERSIONS, version) >= 0;
    }

    public enum Direction {
        SERVERBOUND,
        CLIENTBOUND
    }
}
