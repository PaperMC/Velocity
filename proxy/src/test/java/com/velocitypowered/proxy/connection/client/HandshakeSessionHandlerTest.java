/*
 * Copyright (C) 2018-2021 Velocity Contributors
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

package com.velocitypowered.proxy.connection.client;

import static com.velocitypowered.proxy.connection.client.HandshakeSessionHandler.cleanVhost;
import static com.velocitypowered.proxy.connection.forge.legacy.LegacyForgeConstants.HANDSHAKE_HOSTNAME_TOKEN;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class HandshakeSessionHandlerTest {

  @Test
  void cleanVhostHandlesGoodHostname() {
    assertEquals("localhost", cleanVhost("localhost"));
    assertEquals("mc.example.com", cleanVhost("mc.example.com"));
  }

  @Test
  void cleanVhostHandlesTrailingOctet() {
    assertEquals("localhost", cleanVhost("localhost."));
    assertEquals("mc.example.com", cleanVhost("mc.example.com."));
  }

  @Test
  void cleanVhostHandlesForge() {
    assertEquals("localhost", cleanVhost("localhost" + HANDSHAKE_HOSTNAME_TOKEN));
    assertEquals("mc.example.com", cleanVhost("mc.example.com" + HANDSHAKE_HOSTNAME_TOKEN));
  }

  @Test
  void cleanVhostHandlesOctetsAndForge() {
    assertEquals("localhost", cleanVhost("localhost." + HANDSHAKE_HOSTNAME_TOKEN));
    assertEquals("mc.example.com", cleanVhost("mc.example.com." + HANDSHAKE_HOSTNAME_TOKEN));
  }

  @Test
  void cleanVhostHandlesEmptyHostnames() {
    assertEquals("", cleanVhost(""));
    assertEquals("", cleanVhost(HANDSHAKE_HOSTNAME_TOKEN));
    assertEquals("", cleanVhost("."));
    assertEquals("", cleanVhost("." + HANDSHAKE_HOSTNAME_TOKEN));
  }
}