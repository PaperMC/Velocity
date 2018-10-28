package com.velocitypowered.proxy.connection;

public class VelocityConstants {
    private VelocityConstants() {
        throw new AssertionError();
    }

    public static final String VELOCITY_IP_FORWARDING_CHANNEL = "velocity:player_info";
    public static final int FORWARDING_VERSION = 1;

    public static final byte[] EMPTY_BYTE_ARRAY = new byte[0];
}
