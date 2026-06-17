/*
 * Copyright (C) 2024 Velocity Contributors
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

package com.velocitypowered.proxy.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.velocitypowered.proxy.config.VelocityConfiguration.PacketLimiterConfig;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ConfigurationLoaderTest {

  /**
   * The bundled default config must deserialize cleanly via the ObjectMapper and custom
   * serializers, preserving the dynamic {@code servers}/{@code try} and {@code forced-hosts}
   * sections.
   */
  @Test
  void bundledDefaultLoads(@TempDir final Path dir) throws IOException {
    final Path path = dir.resolve("velocity.yml");
    try (InputStream in = ConfigurationLoaderTest.class.getClassLoader()
        .getResourceAsStream("default-velocity.yml")) {
      assertNotNull(in, "default-velocity.yml is missing from resources");
      Files.copy(in, path);
    }

    final VelocityConfiguration config = ConfigurationLoader.load(path);

    assertEquals(500, config.getShowMaxPlayers());
    assertEquals(ImmutableMap.of(
        "lobby", "127.0.0.1:30066",
        "factions", "127.0.0.1:30067",
        "minigames", "127.0.0.1:30068"), config.getServers());
    assertEquals(ImmutableList.of("lobby"), config.getAttemptConnectionOrder());
    assertEquals(ImmutableMap.of(
        "lobby.example.com", ImmutableList.of("lobby"),
        "factions.example.com", ImmutableList.of("factions"),
        "minigames.example.com", ImmutableList.of("minigames")), config.getForcedHosts());
    assertEquals(7, config.getPacketLimiterConfig().interval());
    assertEquals(5242880, config.getPacketLimiterConfig().bytesAfterDecompression());
    assertTrue(config.getMetrics().isEnabled());
  }

  /**
   * Verifies the non-trivial key mappings (renamed via {@code @Setting} and the custom
   * serializers) using values that differ from the Java field defaults, so a wrong mapping cannot
   * silently fall back to an identical default. Also exercises a save/reload round trip.
   */
  @Test
  void renamedKeysRoundTrip(@TempDir final Path dir) throws IOException {
    final String yaml = """
        config-version: 1
        bind: "0.0.0.0:25577"
        show-max-players: 123
        online-mode: false
        kick-existing-players: true
        player-info-forwarding-mode: "MODERN"
        ping-passthrough: "ALL"
        sample-players-in-ping: true
        enable-player-address-logging: false
        force-key-authentication: false
        announce-forge: true
        packet-limiter:
          interval: 9
          packets-per-second: 100
          bytes-per-second: 200
          decompressed-bytes-per-second: 300
        servers:
          alpha: "1.2.3.4:25565"
          beta: "5.6.7.8:25565"
          try:
            - beta
            - alpha
        forced-hosts:
          "host.example.com":
            - alpha
            - beta
        advanced:
          haproxy-protocol: true
          accepts-transfers: true
          compression-threshold: 128
          command-rate-limit: 99
          enable-reuse-port: true
        query:
          enabled: true
          port: 12345
          map: "CustomMap"
          show-plugins: true
        metrics:
          enabled: false
        """;
    final Path path = dir.resolve("velocity.yml");
    Files.writeString(path, yaml, StandardCharsets.UTF_8);

    assertConfig(ConfigurationLoader.load(path));

    // Round trip: save the loaded config out and read it back; everything must still match.
    final Path roundTripped = dir.resolve("velocity-roundtrip.yml");
    ConfigurationLoader.save(ConfigurationLoader.load(path), roundTripped);
    assertConfig(ConfigurationLoader.load(roundTripped));
  }

  private static void assertConfig(final VelocityConfiguration config) {
    assertEquals(123, config.getShowMaxPlayers());
    assertFalse(config.isOnlineMode());
    assertTrue(config.isOnlineModeKickExistingPlayers());
    assertEquals(PlayerInfoForwarding.MODERN, config.getPlayerInfoForwardingMode());
    assertEquals(PingPassthroughMode.ALL, config.getPingPassthrough());
    assertTrue(config.getSamplePlayersInPing());
    assertFalse(config.isPlayerAddressLoggingEnabled());
    assertFalse(config.isForceKeyAuthentication());
    assertTrue(config.isAnnounceForge());

    final PacketLimiterConfig limiter = config.getPacketLimiterConfig();
    assertEquals(9, limiter.interval());
    assertEquals(100, limiter.pps());
    assertEquals(200, limiter.bytes());
    assertEquals(300, limiter.bytesAfterDecompression());

    assertEquals(ImmutableMap.of(
        "alpha", "1.2.3.4:25565",
        "beta", "5.6.7.8:25565"), config.getServers());
    assertEquals(ImmutableList.of("beta", "alpha"), config.getAttemptConnectionOrder());
    assertEquals(ImmutableMap.of(
        "host.example.com", ImmutableList.of("alpha", "beta")), config.getForcedHosts());

    assertTrue(config.isProxyProtocol());
    assertTrue(config.isAcceptTransfers());
    assertEquals(128, config.getCompressionThreshold());
    assertEquals(99, config.getCommandRatelimit());
    assertTrue(config.isEnableReusePort());

    assertTrue(config.isQueryEnabled());
    assertEquals(12345, config.getQueryPort());
    assertEquals("CustomMap", config.getQueryMap());
    assertTrue(config.shouldQueryShowPlugins());

    assertFalse(config.getMetrics().isEnabled());
  }
}
