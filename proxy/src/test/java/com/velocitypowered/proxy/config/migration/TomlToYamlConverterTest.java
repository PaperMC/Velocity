/*
 * Copyright (C) 2026 Velocity Contributors
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

package com.velocitypowered.proxy.config.migration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.velocitypowered.proxy.config.PingPassthroughMode;
import com.velocitypowered.proxy.config.PlayerInfoForwarding;
import com.velocitypowered.proxy.config.VelocityConfiguration;
import com.velocitypowered.proxy.config.YamlDocument;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.logging.log4j.LogManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class TomlToYamlConverterTest {

  private static final String LEGACY_CONFIGURATION = """
      # Config version. Do not change this
      config-version = "2.1"

      bind = "0.0.0.0:25577"
      motd = "&3A Legacy Server"
      show-max-players = 123
      online-mode = false
      prevent-client-proxy-connections = true
      player-info-forwarding-mode = "MODERN"
      forwarding-secret-file = "forwarding.secret"
      announce-forge = true
      kick-existing-players = true
      ping-passthrough = "all"
      enable-player-address-logging = false

      [servers]
      survival = "127.0.0.1:30070"
      creative = "127.0.0.1:30071"
      try = ["survival"]

      [forced-hosts]
      "Survival.Example.COM" = ["survival"]

      [advanced]
      compression-threshold = 128
      proxy-protocol = true
      command-rate-limit = 77

      [query]
      enabled = true
      port = 25566
      map = "Legacy"
      show-plugins = true
      """;

  private static VelocityConfiguration convert(Path directory) throws IOException {
    Files.writeString(directory.resolve("velocity.toml"), LEGACY_CONFIGURATION,
        StandardCharsets.UTF_8);
    return VelocityConfiguration.read(directory.resolve("velocity.yml"));
  }

  @Test
  void backsUpTheLegacyConfiguration(@TempDir Path directory) throws IOException {
    convert(directory);

    assertFalse(Files.exists(directory.resolve("velocity.toml")));
    assertTrue(Files.exists(directory.resolve("velocity.toml.bak")));
    assertTrue(Files.exists(directory.resolve("velocity.yml")));
    assertEquals(LEGACY_CONFIGURATION,
        Files.readString(directory.resolve("velocity.toml.bak"), StandardCharsets.UTF_8));
  }

  @Test
  void carriesOverConfiguredValues(@TempDir Path directory) throws IOException {
    final VelocityConfiguration configuration = convert(directory);

    assertEquals(25577, configuration.getBind().getPort());
    assertEquals(123, configuration.getShowMaxPlayers());
    assertFalse(configuration.isOnlineMode());
    assertTrue(configuration.shouldPreventClientProxyConnections());
    assertEquals(PlayerInfoForwarding.MODERN, configuration.getPlayerInfoForwardingMode());
    assertTrue(configuration.isAnnounceForge());
    assertTrue(configuration.isOnlineModeKickExistingPlayers());
    assertFalse(configuration.isPlayerAddressLoggingEnabled());
    assertEquals(128, configuration.getCompressionThreshold());
    assertEquals(77, configuration.getCommandRatelimit());
    assertEquals(25566, configuration.getQueryPort());
    assertEquals("Legacy", configuration.getQueryMap());
    assertTrue(configuration.isQueryEnabled());
    assertTrue(configuration.shouldQueryShowPlugins());
  }

  @Test
  void carriesOverServersAndForcedHosts(@TempDir Path directory) throws IOException {
    final VelocityConfiguration configuration = convert(directory);

    assertEquals(Map.of("survival", "127.0.0.1:30070", "creative", "127.0.0.1:30071"),
        configuration.getServers());
    assertEquals(List.of("survival"), configuration.getAttemptConnectionOrder());
    assertEquals(Map.of("survival.example.com", List.of("survival")),
        configuration.getForcedHosts());
  }

  @Test
  void appliesTheLegacyMigrationsBeforeConverting(@TempDir Path directory) throws IOException {
    final VelocityConfiguration configuration = convert(directory);

    assertEquals(new PingPassthroughMode(true, true, true, true, true),
        configuration.getPingPassthrough());
    assertEquals(new VelocityConfiguration.PacketLimiterConfig(7, -1, -1, 5242880),
        configuration.getPacketLimiterConfig());
    assertFalse(configuration.isAcceptTransfers());

    final YamlDocument converted = YamlDocument.load(directory.resolve("velocity.yml"));
    assertTrue(String.valueOf(converted.get("motd")).endsWith("A Legacy Server"),
        String.valueOf(converted.get("motd")));
  }

  @Test
  void alwaysStampsTheConversionAsVersionThree(@TempDir Path directory) throws IOException {
    convert(directory);

    assertEquals(3,
        YamlDocument.load(directory.resolve("velocity.yml")).get("config-version"));
  }

  @Test
  void portsOnlyWhatTheLegacyConfigurationContained(@TempDir Path directory) throws IOException {
    convert(directory);

    final YamlDocument converted = YamlDocument.load(directory.resolve("velocity.yml"));
    final Set<String> ported = converted.leafPaths();

    // A port of the legacy file, not of whatever the proxy currently ships.
    assertFalse(ported.contains("metrics.enabled"), ported.toString());
    assertFalse(ported.contains("advanced.read-timeout"), ported.toString());
    assertTrue(ported.contains("advanced.compression-threshold"), ported.toString());
    assertTrue(ported.contains("packet-limiter.interval"), ported.toString());
    assertTrue(ported.contains("ping-passthrough.modinfo"), ported.toString());
    assertFalse(converted.leafPaths().contains("advanced.proxy-protocol"), ported.toString());
  }

  @Test
  void renamesTheHaproxyProtocolOption(@TempDir Path directory) throws IOException {
    assertTrue(convert(directory).isProxyProtocol());
    assertEquals(true,
        YamlDocument.load(directory.resolve("velocity.yml")).get("advanced.haproxy-protocol"));
  }

  @Test
  void fillsUnconfiguredOptionsFromTheShippedDefaults(@TempDir Path directory) throws IOException {
    final VelocityConfiguration configuration = convert(directory);

    assertEquals(30000, configuration.getReadTimeout());
    assertEquals(-1, configuration.getCompressionLevel());
    assertFalse(configuration.getSamplePlayersInPing());
    assertTrue(configuration.getMetrics().isEnabled());
  }

  @Test
  void writesPlainYamlWithoutBorrowedComments(@TempDir Path directory) throws IOException {
    Files.writeString(directory.resolve("velocity.toml"), LEGACY_CONFIGURATION,
        StandardCharsets.UTF_8);
    TomlToYamlConverter.convert(directory.resolve("velocity.toml"),
        directory.resolve("velocity.yml"), LogManager.getLogger(TomlToYamlConverterTest.class));

    final String converted =
        Files.readString(directory.resolve("velocity.yml"), StandardCharsets.UTF_8);
    for (final String line : converted.split("\n")) {
      assertFalse(line.stripLeading().startsWith("#"), converted);
    }
    assertTrue(converted.startsWith("config-version: 3\n"), converted);
  }

  @Test
  void regainsTheCommentsOfTheShippedConfigurationOnceRead(@TempDir Path directory)
      throws IOException {
    convert(directory);

    final String converted =
        Files.readString(directory.resolve("velocity.yml"), StandardCharsets.UTF_8);
    assertTrue(converted.contains("# Config version. Do not change this\nconfig-version: 3"),
        converted);
    assertTrue(converted.contains(
        "# How large a Minecraft packet has to be before we compress it."), converted);
    assertTrue(converted.contains(
        "# In what order we should try servers when a player logs in or is kicked from a server."),
        converted);
    assertTrue(converted.contains("# Configure your forced hosts here."), converted);
  }

  @Test
  void leavesOptionsTheShippedConfigurationDoesNotKnowUncommented(@TempDir Path directory)
      throws IOException {
    Files.writeString(directory.resolve("velocity.toml"),
        LEGACY_CONFIGURATION.replace("command-rate-limit = 77",
            "command-rate-limit = 77\nlegacy-leftover = true"),
        StandardCharsets.UTF_8);
    VelocityConfiguration.read(directory.resolve("velocity.yml"));

    final YamlDocument converted = YamlDocument.load(directory.resolve("velocity.yml"));
    assertEquals(true, converted.get("advanced.legacy-leftover"));
    assertNull(converted.getComment("advanced.legacy-leftover"));
    assertNotNull(converted.getComment("advanced.command-rate-limit"));
  }

  @Test
  void leavesAnAlreadyConvertedConfigurationAlone(@TempDir Path directory) throws IOException {
    convert(directory);
    final String converted =
        Files.readString(directory.resolve("velocity.yml"), StandardCharsets.UTF_8);

    VelocityConfiguration.read(directory.resolve("velocity.yml"));
    assertEquals(converted,
        Files.readString(directory.resolve("velocity.yml"), StandardCharsets.UTF_8));
  }
}
