package com.velocitypowered.proxy.network;

public class Connections {

  public static final String CIPHER_DECODER = "cipher-decoder";
  public static final String CIPHER_ENCODER = "cipher-encoder";
  public static final String COMPRESSION_DECODER = "compression-decoder";
  public static final String COMPRESSION_ENCODER = "compression-encoder";
  public static final String FLOW_HANDLER = "flow-handler";
  public static final String FRAME_DECODER = "frame-decoder";
  public static final String FRAME_ENCODER = "frame-encoder";
  public static final String HANDLER = "handler";
  public static final String LEGACY_PING_DECODER = "legacy-ping-decoder";
  public static final String LEGACY_PING_ENCODER = "legacy-ping-encoder";
  public static final String MINECRAFT_DECODER = "minecraft-decoder";
  public static final String MINECRAFT_ENCODER = "minecraft-encoder";
  public static final String READ_TIMEOUT = "read-timeout";

  private Connections() {
    throw new AssertionError();
  }
}
