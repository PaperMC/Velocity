package com.velocitypowered.proxy.protocol.util;

import com.velocitypowered.proxy.protocol.packet.ScoreboardObjective;

public class ScoreboardProtocolUtil {
    private ScoreboardProtocolUtil() {
        throw new AssertionError();
    }

    public static ScoreboardObjective.ObjectiveMode getMode(String mode) {
        return ScoreboardObjective.ObjectiveMode.valueOf(mode.toUpperCase());
    }

    public static ScoreboardObjective.ObjectiveMode getMode(int enumVal) {
        switch (enumVal) {
            case 0:
                return ScoreboardObjective.ObjectiveMode.INTEGER;
            case 1:
                return ScoreboardObjective.ObjectiveMode.HEARTS;
            default:
                throw new IllegalStateException("Unknown mode " + enumVal);
        }
    }
}
