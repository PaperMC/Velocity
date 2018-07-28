package com.velocitypowered.proxy.protocol;

import java.util.Arrays;

public enum ProtocolConstants { ;
    public static final int MINECRAFT_1_12 = 340;

    private static final int[] SUPPORTED_VERSIONS = new int[] {
            MINECRAFT_1_12
    };

    public static boolean isSupported(int version) {
        return Arrays.binarySearch(SUPPORTED_VERSIONS, version) >= 0;
    }

    public enum Direction {
        SERVERBOUND,
        CLIENTBOUND
    }
}
