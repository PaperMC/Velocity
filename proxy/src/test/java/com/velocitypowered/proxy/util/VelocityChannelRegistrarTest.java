package com.velocitypowered.proxy.util;

import static org.junit.jupiter.api.Assertions.*;

import com.google.common.collect.ImmutableSet;
import com.velocitypowered.api.proxy.messages.LegacyChannelIdentifier;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import org.junit.jupiter.api.Test;

class VelocityChannelRegistrarTest {

  private static final MinecraftChannelIdentifier MODERN = MinecraftChannelIdentifier
      .create("velocity", "test");
  private static final LegacyChannelIdentifier SIMPLE_LEGACY =
      new LegacyChannelIdentifier("VelocityTest");

  private static final MinecraftChannelIdentifier MODERN_SPECIAL_REMAP = MinecraftChannelIdentifier
      .create("bungeecord", "main");
  private static final LegacyChannelIdentifier SPECIAL_REMAP_LEGACY =
      new LegacyChannelIdentifier("BungeeCord");

  private static final String SIMPLE_LEGACY_REMAPPED = "legacy:velocitytest";

  @Test
  void register() {
    VelocityChannelRegistrar registrar = new VelocityChannelRegistrar();
    registrar.register(MODERN, SIMPLE_LEGACY);

    // Two channels cover the modern channel (velocity:test) and the legacy-mapped channel
    // (legacy:velocitytest). Make sure they're what we expect.
    assertEquals(ImmutableSet.of(MODERN.getId(), SIMPLE_LEGACY_REMAPPED), registrar.getModernChannelIds());
    assertEquals(ImmutableSet.of(SIMPLE_LEGACY.getId(), MODERN.getId()), registrar.getLegacyChannelIds());
  }

  @Test
  void registerSpecialRewrite() {
    VelocityChannelRegistrar registrar = new VelocityChannelRegistrar();
    registrar.register(SPECIAL_REMAP_LEGACY, MODERN_SPECIAL_REMAP);

    // This one, just one channel for the modern case.
    assertEquals(ImmutableSet.of(MODERN_SPECIAL_REMAP.getId()), registrar.getModernChannelIds());
    assertEquals(ImmutableSet.of(MODERN_SPECIAL_REMAP.getId(), SPECIAL_REMAP_LEGACY.getId()),
        registrar.getLegacyChannelIds());
  }

  @Test
  void unregister() {
    VelocityChannelRegistrar registrar = new VelocityChannelRegistrar();
    registrar.register(MODERN, SIMPLE_LEGACY);
    registrar.unregister(SIMPLE_LEGACY);

    assertEquals(ImmutableSet.of(MODERN.getId()), registrar.getModernChannelIds());
    assertEquals(ImmutableSet.of(MODERN.getId()), registrar.getLegacyChannelIds());
  }
}