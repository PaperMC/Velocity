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

import static com.velocitypowered.api.event.connection.DisconnectEvent.LoginStatus.PRE_SERVER_JOIN;
import static com.velocitypowered.api.event.connection.DisconnectEvent.LoginStatus.SUCCESSFUL_LOGIN;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Answers.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;

import com.velocitypowered.proxy.connection.backend.VelocityServerConnection;
import org.junit.jupiter.api.Test;

class ConnectedPlayerTest {

  @Test
  void backendDisconnectAfterServerJoinIsSuccessfulLogin() {
    ConnectedPlayer player = mock(ConnectedPlayer.class, CALLS_REAL_METHODS);
    player.setConnectedServer(mock(VelocityServerConnection.class));
    player.setConnectedServer(null);

    assertEquals(SUCCESSFUL_LOGIN, player.determineLoginStatus(player, true));
  }

  @Test
  void disconnectBeforeServerJoinIsPreServerJoin() {
    ConnectedPlayer player = mock(ConnectedPlayer.class, CALLS_REAL_METHODS);

    assertEquals(PRE_SERVER_JOIN, player.determineLoginStatus(player, true));
  }
}
