package com.velocitypowered.proxy.protocol.util;

import com.velocitypowered.proxy.data.scoreboard.ObjectiveMode;

public class ScoreboardProtocolUtil {
    private ScoreboardProtocolUtil() {
        throw new AssertionError();
    }

    public static ObjectiveMode getMode(String mode) {
        return ObjectiveMode.valueOf(mode.toUpperCase());
    }

    public static ObjectiveMode getMode(int enumVal) {
        switch (enumVal) {
            case 0:
                return ObjectiveMode.INTEGER;
            case 1:
                return ObjectiveMode.HEARTS;
            default:
                throw new IllegalStateException("Unknown mode " + enumVal);
        }
    }
}
