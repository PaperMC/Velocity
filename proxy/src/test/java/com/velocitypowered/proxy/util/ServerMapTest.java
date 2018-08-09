package com.velocitypowered.proxy.util;

import com.velocitypowered.api.server.ServerInfo;
import org.junit.jupiter.api.Test;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class ServerMapTest {
    private static final InetSocketAddress TEST_ADDRESS = new InetSocketAddress(InetAddress.getLoopbackAddress(), 25565);

    @Test
    void respectsCaseInsensitivity() {
        ServerMap map = new ServerMap();
        ServerInfo info = new ServerInfo("TestServer", TEST_ADDRESS);
        map.register(info);

        assertEquals(Optional.of(info), map.getServer("TestServer"));
        assertEquals(Optional.of(info), map.getServer("testserver"));
        assertEquals(Optional.of(info), map.getServer("TESTSERVER"));
    }

    @Test
    void rejectsRepeatedRegisterAttempts() {
        ServerMap map = new ServerMap();
        ServerInfo info = new ServerInfo("TestServer", TEST_ADDRESS);
        map.register(info);

        ServerInfo willReject = new ServerInfo("TESTSERVER", TEST_ADDRESS);
        assertThrows(IllegalArgumentException.class, () -> map.register(willReject));
    }
}