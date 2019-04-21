package com.velocitypowered.api.proxy.messages;

import static com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier.create;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class MinecraftChannelIdentifierTest {

  @Test
  void createAllowsValidNamespaces() {
    create("minecraft", "brand");
  }

  @Test
  void createAllowsEmptyName() {
    create("minecraft", "");
  }

  @Test
  void createDisallowsNull() {
    assertAll(
        () -> assertThrows(IllegalArgumentException.class, () -> create(null, "")),
        () -> assertThrows(IllegalArgumentException.class, () -> create("", "")),
        () -> assertThrows(IllegalArgumentException.class, () -> create("minecraft", null))
    );
  }
}