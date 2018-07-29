package com.velocitypowered.proxy.protocol;

import java.util.Arrays;

public enum ProtocolConstants { ;
    public static final int MINECRAFT_1_12 = 335;
    public static final int MINECRAFT_1_12_1 = 338;
    public static final int MINECRAFT_1_12_2 = 340;

    public static final int[] SUPPORTED_VERSIONS = new int[] {
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
