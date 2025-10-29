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

package com.velocitypowered.proxy.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.proxy.server.ServerInfo;
import com.velocitypowered.proxy.server.ServerMap;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ServerMapTest {

  private static final InetSocketAddress TEST_ADDRESS = new InetSocketAddress(
      InetAddress.getLoopbackAddress(), 25565);

  @Test
  void respectsCaseInsensitivity() {
    ServerMap map = new ServerMap(null);
    ServerInfo info = new ServerInfo("TestServer", TEST_ADDRESS);
    RegisteredServer connection = map.register(info);

    assertEquals(Optional.of(connection), map.getServer("TestServer"));
    assertEquals(Optional.of(connection), map.getServer("testserver"));
    assertEquals(Optional.of(connection), map.getServer("TESTSERVER"));
  }

  @Test
  void rejectsRepeatedRegisterAttempts() {
    ServerMap map = new ServerMap(null);
    ServerInfo info = new ServerInfo("TestServer", TEST_ADDRESS);
    map.register(info);

    ServerInfo willReject = new ServerInfo("TESTSERVER", TEST_ADDRESS);
    assertThrows(IllegalArgumentException.class, () -> map.register(willReject));
  }

  @Test
  void allowsSameServerLaxRegistrationCheck() {
    ServerMap map = new ServerMap(null);
    ServerInfo info = new ServerInfo("TestServer", TEST_ADDRESS);
    RegisteredServer connection = map.register(info);
    assertEquals(connection, map.register(info));
  }
}