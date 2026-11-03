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

package com.velocitypowered.proxy.tablist;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.velocitypowered.api.proxy.player.TabListEntry;
import com.velocitypowered.api.util.GameProfile;
import com.velocitypowered.proxy.connection.MinecraftConnection;
import com.velocitypowered.proxy.connection.client.ConnectedPlayer;
import com.velocitypowered.proxy.protocol.packet.RemovePlayerInfoPacket;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class VelocityTabListTest {

  private MinecraftConnection connection;
  private VelocityTabList tabList;

  @BeforeEach
  void setUp() {
    connection = mock(MinecraftConnection.class);
    ConnectedPlayer player = mock(ConnectedPlayer.class);
    when(player.getConnection()).thenReturn(connection);
    tabList = new VelocityTabList(player);
  }

  @Test
  void removingUnknownEntryDoesNotSendUuid() {
    assertTrue(tabList.removeEntry(UUID.randomUUID()).isEmpty());
    verifyNoInteractions(connection);
  }

  @Test
  void removingExistingEntrySendsUuidOnlyOnce() {
    UUID uuid = UUID.randomUUID();
    TabListEntry entry = tabList.buildEntry(new GameProfile(uuid, "TestPlayer", List.of()),
        null, 0, 0, null, true, 0, true);
    tabList.addEntry(entry);
    clearInvocations(connection);

    assertSame(entry, tabList.removeEntry(uuid).orElseThrow());
    assertFalse(tabList.containsEntry(uuid));

    ArgumentCaptor<RemovePlayerInfoPacket> packet =
        ArgumentCaptor.forClass(RemovePlayerInfoPacket.class);
    verify(connection).write(packet.capture());
    assertEquals(List.of(uuid), packet.getValue().getProfilesToRemove());

    assertTrue(tabList.removeEntry(uuid).isEmpty());
    verifyNoMoreInteractions(connection);
  }
}
