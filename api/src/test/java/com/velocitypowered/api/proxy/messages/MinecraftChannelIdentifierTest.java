package com.velocitypowered.api.proxy.messages;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class MinecraftChannelIdentifierTest {

  @Test
  void createAllowsValidNamespaces() {
    MinecraftChannelIdentifier.create("minecraft", "brand");
  }

  @Test
  void createAllowsEmptyName() {
    MinecraftChannelIdentifier.create("minecraft", "");
  }

  @Test
  void createDisallowsNull() {
    assertAll(
        () -> assertThrows(IllegalArgumentException.class, () -> MinecraftChannelIdentifier.create(null, "")),
        () -> assertThrows(IllegalArgumentException.class, () -> MinecraftChannelIdentifier.create("", "")),
        () -> assertThrows(IllegalArgumentException.class, () -> MinecraftChannelIdentifier.create("minecraft", null))
    );
  }
}