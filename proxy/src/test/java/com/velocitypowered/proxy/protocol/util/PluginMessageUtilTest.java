package com.velocitypowered.proxy.protocol.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class PluginMessageUtilTest {

  @Test
  void transformLegacyToModernChannelWorksWithModern() {
    assertEquals("minecraft:brand", PluginMessageUtil
        .transformLegacyToModernChannel("minecraft:brand"));
    assertEquals("velocity:test", PluginMessageUtil
        .transformLegacyToModernChannel("velocity:test"));
  }

  @Test
  void transformLegacyToModernChannelRewritesSpecialCases() {
    assertEquals("minecraft:brand", PluginMessageUtil
        .transformLegacyToModernChannel("MC|Brand"));
    assertEquals("minecraft:register", PluginMessageUtil
        .transformLegacyToModernChannel("REGISTER"));
    assertEquals("minecraft:unregister", PluginMessageUtil
        .transformLegacyToModernChannel("UNREGISTER"));
    assertEquals("bungeecord:main", PluginMessageUtil
        .transformLegacyToModernChannel("BungeeCord"));
  }

  @Test
  void transformLegacyToModernChannelRewritesGeneral() {
    assertEquals("legacy:example", PluginMessageUtil
        .transformLegacyToModernChannel("Example"));
    assertEquals("legacy:pskeepalive", PluginMessageUtil
        .transformLegacyToModernChannel("PS|KeepAlive"));
  }
}