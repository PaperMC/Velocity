package com.velocitypowered.proxy.connection.forge;

import com.velocitypowered.proxy.protocol.packet.PluginMessage;

public class ForgeConstants {

  public static final String FORGE_LEGACY_HANDSHAKE_CHANNEL = "FML|HS";
  public static final String FORGE_LEGACY_CHANNEL = "FML";
  public static final String FORGE_MULTIPART_LEGACY_CHANNEL = "FML|MP";
  private static final byte[] FORGE_LEGACY_HANDSHAKE_RESET_DATA = new byte[]{-2, 0};

  private ForgeConstants() {
    throw new AssertionError();
  }

  public static PluginMessage resetPacket() {
    PluginMessage msg = new PluginMessage();
    msg.setChannel(FORGE_LEGACY_HANDSHAKE_CHANNEL);
    msg.setData(FORGE_LEGACY_HANDSHAKE_RESET_DATA.clone());
    return msg;
  }
}
