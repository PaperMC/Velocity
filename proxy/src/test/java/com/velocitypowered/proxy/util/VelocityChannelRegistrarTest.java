/*
 * Copyright (C) 2018 Velocity Contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.velocitypowered.proxy.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.google.common.collect.ImmutableSet;
import com.velocitypowered.api.proxy.messages.KeyedPluginChannelId;
import com.velocitypowered.api.proxy.messages.PairedPluginChannelId;
import com.velocitypowered.api.proxy.messages.PluginChannelId;
import net.kyori.adventure.key.Key;
import org.junit.jupiter.api.Test;

class VelocityChannelRegistrarTest {

  private static final KeyedPluginChannelId MODERN = PluginChannelId.wrap(
      Key.key("velocity", "moderntest"));
  private static final PairedPluginChannelId SIMPLE_LEGACY =
      PluginChannelId.withLegacy("VelocityTest", Key.key("velocity", "test"));

  @Test
  void register() {
    VelocityChannelRegistrar registrar = new VelocityChannelRegistrar();
    registrar.register(MODERN, SIMPLE_LEGACY);

    // Two channels cover the modern channel (velocity:test) and the legacy-mapped channel
    // (legacy:velocitytest). Make sure they're what we expect.
    assertEquals(
        ImmutableSet.of(MODERN.key().asString(), SIMPLE_LEGACY.modernChannelKey().asString()),
        registrar.getModernChannelIds());
    assertEquals(
        ImmutableSet.of(SIMPLE_LEGACY.legacyChannel(), MODERN.key().asString()), registrar
        .getLegacyChannelIds());
  }

  @Test
  void unregister() {
    VelocityChannelRegistrar registrar = new VelocityChannelRegistrar();
    registrar.register(MODERN);
    registrar.unregister(SIMPLE_LEGACY);

    assertEquals(ImmutableSet.of(MODERN.key().asString()), registrar.getModernChannelIds());
    assertEquals(ImmutableSet.of(MODERN.key().asString()), registrar.getLegacyChannelIds());
  }
}