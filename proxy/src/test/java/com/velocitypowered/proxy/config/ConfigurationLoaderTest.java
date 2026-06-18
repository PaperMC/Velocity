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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
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
import org.spongepowered.configurate.CommentedConfigurationNode;

class ConfigurationLoaderTest {

  /**
   * The bundled default config must deserialize cleanly via the ObjectMapper and custom
   * serializers, preserving the dynamic {@code servers}/{@code try} and {@code forced-hosts}
   * sections.
   */
  @Test
  void bundledDefaultLoads(@TempDir final Path dir) throws IOException {
    final Path path = dir.resolve("velocity.yaml");
    try (InputStream in = ConfigurationLoaderTest.class.getClassLoader()
        .getResourceAsStream("default-velocity.yaml")) {
      assertNotNull(in, "default-velocity.yaml is missing from resources");
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
    final Path path = dir.resolve("velocity.yaml");
    Files.writeString(path, yaml, StandardCharsets.UTF_8);

    assertConfig(ConfigurationLoader.load(path));

    // Round trip: save the loaded config out and read it back; everything must still match.
    final Path roundTripped = dir.resolve("velocity-roundtrip.yaml");
    ConfigurationLoader.save(ConfigurationLoader.load(path), roundTripped);
    assertConfig(ConfigurationLoader.load(roundTripped));
  }

  /**
   * A legacy {@code velocity.toml} must be converted to {@code velocity.yaml}: values carried over,
   * the schema version stamped, a custom forwarding-secret-file preserved, and the old file
   * archived as {@code velocity.toml.migrated}.
   */
  @Test
  void migratesLegacyTomlToYaml(@TempDir final Path dir) throws IOException {
    final Path secret = dir.resolve("forwarding.secret");
    Files.writeString(secret, "supersecretvalue");

    final Path toml = dir.resolve("velocity.toml");
    final String secretLiteral = secret.toString().replace("\\", "\\\\");
    Files.writeString(toml, """
        config-version = "2.8"
        bind = "0.0.0.0:25599"
        show-max-players = 321
        online-mode = false
        player-info-forwarding-mode = "MODERN"
        forwarding-secret-file = "%s"

        [servers]
        hub = "10.0.0.1:25565"
        try = ["hub"]

        [advanced]
        haproxy-protocol = true
        accepts-transfers = true
        """.formatted(secretLiteral));

    final Path yaml = dir.resolve("velocity.yaml");
    final VelocityConfiguration config = ConfigurationLoader.loadConfiguration(yaml, toml);

    assertTrue(Files.exists(yaml));
    assertFalse(Files.exists(toml));
    assertTrue(Files.exists(dir.resolve("velocity.toml.migrated")));

    assertEquals(321, config.getShowMaxPlayers());
    assertFalse(config.isOnlineMode());
    assertEquals(PlayerInfoForwarding.MODERN, config.getPlayerInfoForwardingMode());
    assertEquals(ImmutableMap.of("hub", "10.0.0.1:25565"), config.getServers());
    assertEquals(ImmutableList.of("hub"), config.getAttemptConnectionOrder());
    assertTrue(config.isProxyProtocol());
    assertTrue(config.isAcceptTransfers());
    assertArrayEquals("supersecretvalue".getBytes(StandardCharsets.UTF_8),
        config.getForwardingSecret());

    final CommentedConfigurationNode node = ConfigurationLoader.yamlLoader(yaml).build().load();
    assertEquals(ConfigurationLoader.CURRENT_CONFIG_VERSION,
        node.node(ConfigurationLoader.CONFIG_VERSION_KEY).getInt());
    assertEquals(secret.toString(), node.node("forwarding-secret-file").getString());
  }

  /**
   * Removing the {@code forced-hosts}/{@code try} sections must leave them empty rather than
   * resurrecting the bundled example entries, which would reference servers the user does not have
   * and fail validation.
   */
  @Test
  void removedSectionsDoNotResurrectDefaults(@TempDir final Path dir) throws IOException {
    final Path path = dir.resolve("velocity.yaml");
    Files.writeString(path, """
        servers:
          only: "1.2.3.4:25565"
        """, StandardCharsets.UTF_8);

    final VelocityConfiguration config = ConfigurationLoader.load(path);

    assertEquals(ImmutableMap.of("only", "1.2.3.4:25565"), config.getServers());
    assertTrue(config.getForcedHosts().isEmpty());
    assertTrue(config.getAttemptConnectionOrder().isEmpty());
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
