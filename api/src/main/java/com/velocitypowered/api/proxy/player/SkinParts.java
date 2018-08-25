package com.velocitypowered.api.proxy.player;

public class SkinParts {

    static final SkinParts SKIN_SHOW_ALL = new SkinParts((byte) 127);
    private final byte bitmask;

    public SkinParts(byte skinBitmask) {
        this.bitmask = skinBitmask;
    }

    public boolean hasCape() {
        return ((bitmask >> 0) & 1) == 1;
    }

    public boolean hasJacket() {
        return ((bitmask >> 1) & 1) == 1;
    }

    public boolean hasLeftSleeve() {
        return ((bitmask >> 2) & 1) == 1;
    }

    public boolean hasRightSleeve() {
        return ((bitmask >> 3) & 1) == 1;
    }

    public boolean hasLeftPants() {
        return ((bitmask >> 4) & 1) == 1;
    }

    public boolean hasRightPants() {
        return ((bitmask >> 5) & 1) == 1;
    }

    public boolean hasHat() {
        return ((bitmask >> 6) & 1) == 1;
    }
}
