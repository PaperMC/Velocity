package com.velocitypowered.proxy.connection.forge;

public class ForgeConstants {
    public static final String FORGE_LEGACY_HANDSHAKE_CHANNEL = "FML|HS";
    public static final String FORGE_LEGACY_CHANNEL = "FML";
    public static final String FORGE_MULTIPART_LEGACY_CHANNEL = "FML|MP";
    public static final byte[] FORGE_LEGACY_HANDSHAKE_RESET_DATA = new byte[] { -2, 0 };

    private ForgeConstants() {
        throw new AssertionError();
    }
}
