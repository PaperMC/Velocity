/*
 * Copyright (C) 2019-2021 Velocity Contributors
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