package com.velocitypowered.proxy.network;

public interface Connections {
    String CIPHER_DECODER = "cipher-decoder";
    String CIPHER_ENCODER = "cipher-encoder";
    String COMPRESSION_DECODER = "compression-decoder";
    String COMPRESSION_ENCODER = "compression-encoder";
    String FRAME_DECODER = "frame-decoder";
    String FRAME_ENCODER = "frame-encoder";
    String HANDLER = "handler";
    String LEGACY_PING_DECODER = "legacy-ping-decoder";
    String LEGACY_PING_ENCODER = "legacy-ping-encoder";
    String MINECRAFT_DECODER = "minecraft-decoder";
    String MINECRAFT_ENCODER = "minecraft-encoder";
    String READ_TIMEOUT = "read-timeout";

    int CLIENT_READ_TIMEOUT_SECONDS = 30; // client -> proxy
    int SERVER_READ_TIMEOUT_SECONDS = 30; // proxy -> server
}
