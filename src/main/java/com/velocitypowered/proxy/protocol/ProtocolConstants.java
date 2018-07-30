package com.velocitypowered.proxy.protocol;

import java.util.Arrays;

public enum ProtocolConstants { ;
    public static final int MINECRAFT_1_7_2 = 4;
    public static final int MINECRAFT_1_10 = 210;
    public static final int MINECRAFT_1_11 = 315;
    public static final int MINECRAFT_1_11_1 = 316;
    public static final int MINECRAFT_1_12 = 335;
    public static final int MINECRAFT_1_12_1 = 338;
    public static final int MINECRAFT_1_12_2 = 340;

    public static final int MINIMUM_VERSION_SUPPORTED = MINECRAFT_1_10;
    public static final int MINIMUM_GENERIC_VERSION = MINECRAFT_1_10;

    public static final int[] SUPPORTED_VERSIONS = new int[] {
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
