package com.velocitypowered.proxy.connection;

public class VelocityConstants {
    private VelocityConstants() {
        throw new AssertionError();
    }

    public static final String VELOCITY_IP_FORWARDING_CHANNEL = "velocity:player_info";

    public static final String FORGE_LEGACY_HANDSHAKE_CHANNEL = "FML|HS";
    public static final String FORGE_LEGACY_CHANNEL = "FML";
    public static final String FORGE_MULTIPART_LEGACY_CHANNEL = "FML|MP";

    public static final byte[] FORGE_LEGACY_HANDSHAKE_RESET_DATA = new byte[] { -2, 0 };
}
