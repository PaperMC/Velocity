package com.velocitypowered.proxy.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.velocitypowered.api.proxy.config.PlayerInfoForwarding;
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
    ServerInfo info = new ServerInfo("TestServer", TEST_ADDRESS, PlayerInfoForwarding.NONE);
    RegisteredServer connection = map.register(info);

    assertEquals(Optional.of(connection), map.getServer("TestServer"));
    assertEquals(Optional.of(connection), map.getServer("testserver"));
    assertEquals(Optional.of(connection), map.getServer("TESTSERVER"));
  }

  @Test
  void rejectsRepeatedRegisterAttempts() {
    ServerMap map = new ServerMap(null);
    ServerInfo info = new ServerInfo("TestServer", TEST_ADDRESS, PlayerInfoForwarding.NONE);
    map.register(info);

    ServerInfo willReject = new ServerInfo("TESTSERVER", TEST_ADDRESS, PlayerInfoForwarding.NONE);
    assertThrows(IllegalArgumentException.class, () -> map.register(willReject));
  }

  @Test
  void allowsSameServerLaxRegistrationCheck() {
    ServerMap map = new ServerMap(null);
    ServerInfo info = new ServerInfo("TestServer", TEST_ADDRESS, PlayerInfoForwarding.NONE);
    RegisteredServer connection = map.register(info);
    assertEquals(connection, map.register(info));
  }
}