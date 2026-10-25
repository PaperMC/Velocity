/*
 * Copyright (C) 2018-2026 Velocity Contributors
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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.velocitypowered.api.util.UuidUtils;
import com.velocitypowered.proxy.config.PlayerInfoForwarding;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ConnectedPlayerClientOnlineModeTest {

  private static final UUID MOJANG_ID = UUID.fromString("8d6d0684-d8b4-4d40-8d2d-0dd4df5555c8");
  private static final UUID OFFLINE_ID = UuidUtils.generateOfflinePlayerUuid("Alice");

  @Test
  void offlinePlayerIsNeverReportedOnline() {
    assertFalse(clientOnlineMode(false, true, PlayerInfoForwarding.MODERN, OFFLINE_ID, null));
    assertFalse(clientOnlineMode(false, false, PlayerInfoForwarding.MODERN, OFFLINE_ID, null));
  }

  @Test
  void reportsRealOnlineModeWhenOptionIsDisabled() {
    assertTrue(clientOnlineMode(true, false, PlayerInfoForwarding.MODERN, OFFLINE_ID, MOJANG_ID));
    assertTrue(clientOnlineMode(true, false, PlayerInfoForwarding.NONE, MOJANG_ID, MOJANG_ID));
  }

  @Test
  void staysOnlineWhenProfileUuidMatchesSession() {
    assertTrue(clientOnlineMode(true, true, PlayerInfoForwarding.MODERN, MOJANG_ID, MOJANG_ID));
  }

  @Test
  void reportsOfflineWhenPluginRewroteUuid() {
    assertFalse(clientOnlineMode(true, true, PlayerInfoForwarding.MODERN, OFFLINE_ID, MOJANG_ID));
  }

  @Test
  void reportsOfflineWhenForwardingIsDisabled() {
    // Without forwarding the backend derives the offline UUID itself, whatever the profile says
    assertFalse(clientOnlineMode(true, true, PlayerInfoForwarding.NONE, MOJANG_ID, MOJANG_ID));
  }

  private static boolean clientOnlineMode(boolean onlineMode, boolean requireMatchingUuid,
      PlayerInfoForwarding forwarding, UUID uniqueId, UUID authenticatedId) {
    return ConnectedPlayer.isClientOnlineMode(onlineMode, requireMatchingUuid, forwarding, "Alice",
        uniqueId, authenticatedId);
  }
}
