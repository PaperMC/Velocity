package com.velocitypowered.proxy.connection.client;

import static com.velocitypowered.proxy.connection.client.HandshakeSessionHandler.cleanVhost;
import static com.velocitypowered.proxy.connection.forge.legacy.LegacyForgeConstants.*;
import static org.junit.jupiter.api.Assertions.*;

import com.velocitypowered.proxy.connection.forge.legacy.LegacyForgeConstants;
import org.junit.jupiter.api.Test;

class HandshakeSessionHandlerTest {

  @Test
  void cleanVhostHandlesGoodHostname() {
    assertEquals("localhost", cleanVhost("localhost"));
  }

  @Test
  void cleanVhostHandlesTrailingOctet() {
    assertEquals("localhost", cleanVhost("localhost."));
  }

  @Test
  void cleanVhostHandlesForge() {
    assertEquals("localhost", cleanVhost("localhost" + HANDSHAKE_HOSTNAME_TOKEN));
  }

  @Test
  void cleanVhostHandlesOctetsAndForge() {
    assertEquals("localhost", cleanVhost("localhost." + HANDSHAKE_HOSTNAME_TOKEN));
  }
}