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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.velocitypowered.proxy.config.PingPassthroughMode;
import com.velocitypowered.proxy.config.PlayerInfoForwarding;
import com.velocitypowered.proxy.config.VelocityConfiguration;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

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
      kick-existing-players = true
      ping-passthrough = "all"
      enable-player-address-logging = false

      [servers]
      survival = "127.0.0.1:30070"
      "creative.1" = "127.0.0.1:30071"
      try = ["survival"]

      [forced-hosts]
      "Survival.Example.COM" = ["survival"]

      [advanced]
      compression-threshold = 128
      proxy-protocol = true
      tab-complete-rate-limit = 20

      [query]
      enabled = true
      port = 25566
      map = "Legacy"
      show-plugins = true
      """;

  private static VelocityConfiguration convert(Path directory) throws IOException {
    return convert(directory, LEGACY_CONFIGURATION);
  }

  private static VelocityConfiguration convert(Path directory, String legacy)
      throws IOException {
    Files.writeString(directory.resolve("forwarding.secret"), "secret");
    Files.writeString(directory.resolve("velocity.toml"), legacy, StandardCharsets.UTF_8);
    return VelocityConfiguration.read(directory.resolve("velocity.yaml"));
  }

  private static CommentedConfigurationNode converted(Path directory) throws IOException {
    return YamlConfigurationLoader.builder().path(directory.resolve("velocity.yaml")).build()
        .load();
  }

  private static String convertedText(Path directory) throws IOException {
    return Files.readString(directory.resolve("velocity.yaml"), StandardCharsets.UTF_8);
  }

  @Test
  void backsUpTheLegacyConfiguration(@TempDir Path directory) throws IOException {
    convert(directory);

    assertFalse(Files.exists(directory.resolve("velocity.toml")));
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
    assertTrue(configuration.isOnlineModeKickExistingPlayers());
    assertFalse(configuration.isPlayerAddressLoggingEnabled());
    assertEquals(128, configuration.getCompressionThreshold());
    assertEquals(20, configuration.getTabCompleteRatelimit());
    assertTrue(configuration.isQueryEnabled());
    assertEquals(25566, configuration.getQueryPort());
    assertEquals("Legacy", configuration.getQueryMap());
    assertTrue(configuration.shouldQueryShowPlugins());
  }

  @Test
  void carriesOverServersAndForcedHosts(@TempDir Path directory) throws IOException {
    final VelocityConfiguration configuration = convert(directory);

    assertEquals(Map.of("survival", "127.0.0.1:30070", "creative.1", "127.0.0.1:30071"),
        configuration.getServers());
    assertEquals(List.of("survival"), configuration.getAttemptConnectionOrder());
    assertEquals(Map.of("survival.example.com", List.of("survival")),
        configuration.getForcedHosts());
  }

  @Test
  void appliesTheLegacyMigrationsFirst(@TempDir Path directory) throws IOException {
    final VelocityConfiguration configuration = convert(directory);

    assertEquals(new PingPassthroughMode(true, true, true, true, true),
        configuration.getPingPassthrough());
    assertEquals(new VelocityConfiguration.PacketLimiterConfig(7, -1, -1, 5242880),
        configuration.getPacketLimiterConfig());
    assertFalse(configuration.isAcceptTransfers());
    assertEquals("<dark_aqua>A Legacy Server",
        converted(directory).node("motd").getString());
  }

  @Test
  void stampsVersionThree(@TempDir Path directory) throws IOException {
    convert(directory);

    assertEquals(3, converted(directory).node("config-version").getInt());
  }

  @Test
  void portsOnlyWhatTheLegacyConfigurationContained(@TempDir Path directory)
      throws IOException {
    convert(directory);
    final CommentedConfigurationNode converted = converted(directory);

    assertTrue(converted.node("metrics").virtual());
    assertTrue(converted.node("advanced", "read-timeout").virtual());
    assertFalse(converted.node("advanced", "compression-threshold").virtual());
    assertFalse(converted.node("packet-limiter", "interval").virtual());
    assertFalse(converted.node("ping-passthrough", "modinfo").virtual());
  }

  @Test
  void renamesTheProxyProtocolOption(@TempDir Path directory) throws IOException {
    assertTrue(convert(directory).isProxyProtocol());

    final CommentedConfigurationNode converted = converted(directory);
    assertTrue(converted.node("advanced", "haproxy-protocol").getBoolean());
    assertTrue(converted.node("advanced", "proxy-protocol").virtual());
  }

  @Test
  void movesTheFallbackOrderOutOfTheServers(@TempDir Path directory) throws IOException {
    convert(directory);
    final CommentedConfigurationNode converted = converted(directory);

    assertEquals(List.of("survival"), converted.node("try").getList(String.class));
    assertTrue(converted.node("servers", "try").virtual());
    assertTrue(convertedText(directory).contains("""
          creative.1: "127.0.0.1:30071"

        # In what order we should try servers when a player logs in or is kicked from a server.
        try:
          - survival
        """), convertedText(directory));
  }

  @Test
  void groupsTheForwardingOptions(@TempDir Path directory) throws IOException {
    convert(directory);
    final CommentedConfigurationNode converted = converted(directory);

    assertEquals("MODERN", converted.node("forwarding", "mode").getString());
    assertEquals("forwarding.secret",
        converted.node("forwarding", "secret-file").getString());
    assertTrue(converted.node("player-info-forwarding-mode").virtual());
    assertTrue(converted.node("forwarding-secret-file").virtual());
  }

  @Test
  void groupsTheRateLimits(@TempDir Path directory) throws IOException {
    convert(directory);
    final CommentedConfigurationNode converted = converted(directory);

    assertEquals(20, converted.node("rate-limits", "tab-completes").getInt());
    assertEquals(25, converted.node("rate-limits", "commands").getInt());
    assertTrue(converted.node("advanced", "tab-complete-rate-limit").virtual());
    assertTrue(converted.node("advanced", "command-rate-limit").virtual());
  }

  @Test
  void groupsTheLoggingOptions(@TempDir Path directory) throws IOException {
    convert(directory);
    final CommentedConfigurationNode converted = converted(directory);

    assertFalse(converted.node("logging", "player-addresses").getBoolean());
    assertTrue(converted.node("enable-player-address-logging").virtual());
  }

  @Test
  void keepsWhatTheTomlReaderFellBackToForMissingOptions(@TempDir Path directory)
      throws IOException {
    final VelocityConfiguration configuration = convert(directory);

    assertTrue(configuration.isAnnounceForge());
    assertEquals(25, configuration.getCommandRatelimit());
    assertEquals(0, configuration.getKickAfterRateLimitedCommands());
    assertEquals(0, configuration.getKickAfterRateLimitedTabCompletes());
  }

  @Test
  void keepsTheFieldDefaultsUsedWithoutAnAdvancedTable(@TempDir Path directory)
      throws IOException {
    final VelocityConfiguration configuration = convert(directory, """
        config-version = "2.9"
        announce-forge = false
        """);

    assertFalse(configuration.isAnnounceForge());
    assertEquals(50, configuration.getCommandRatelimit());
    assertEquals(5, configuration.getKickAfterRateLimitedCommands());
    assertEquals(50, configuration.getTabCompleteRatelimit());
    assertEquals(10, configuration.getKickAfterRateLimitedTabCompletes());
  }

  @Test
  void fillsUnconfiguredOptionsFromTheShippedDefaults(@TempDir Path directory)
      throws IOException {
    final VelocityConfiguration configuration = convert(directory);

    assertEquals(30000, configuration.getReadTimeout());
    assertEquals(-1, configuration.getCompressionLevel());
    assertFalse(configuration.getSamplePlayersInPing());
    assertTrue(configuration.getMetrics().isEnabled());
  }

  @Test
  void documentsTheConvertedOptionsLikeTheShippedConfiguration(@TempDir Path directory)
      throws IOException {
    convert(directory);
    final String converted = convertedText(directory);

    assertTrue(converted.startsWith("# Config version. Do not change this.\nconfig-version: 3\n"),
        converted);
    assertTrue(converted.contains("\n  # How large a Minecraft packet has to be before we "
        + "compress it."), converted);
    assertTrue(converted.contains("# Configure your forced hosts here.\nforced-hosts:"),
        converted);
    assertTrue(converted.contains("bind: \"0.0.0.0:25577\""), converted);
  }

  @Test
  void leavesUnknownOptionsUncommented(@TempDir Path directory) throws IOException {
    convert(directory, LEGACY_CONFIGURATION.replace("compression-threshold = 128",
        "compression-threshold = 128\nlegacy-leftover = true"));
    final CommentedConfigurationNode converted = converted(directory);

    assertTrue(converted.node("advanced", "legacy-leftover").getBoolean());
    assertNull(converted.node("advanced", "legacy-leftover").comment());
  }

  @Test
  void convertsIntoAnEmptyFileInPlace(@TempDir Path directory) throws IOException {
    // A Docker user mounts an empty velocity.yaml next to their velocity.toml.
    Files.writeString(directory.resolve("velocity.yaml"), "");
    assertEquals(123, convert(directory).getShowMaxPlayers());
  }

  @Test
  void leavesAnAlreadyConvertedConfigurationAlone(@TempDir Path directory) throws IOException {
    convert(directory);
    final String converted = convertedText(directory);

    VelocityConfiguration.read(directory.resolve("velocity.yaml"));
    assertEquals(converted, convertedText(directory));
  }

  @Test
  void ignoresTheTomlOnceThereIsYamlConfiguration(@TempDir Path directory) throws IOException {
    Files.writeString(directory.resolve("velocity.yaml"), "config-version: 3\n");

    assertEquals(500, convert(directory).getShowMaxPlayers());
    assertTrue(Files.exists(directory.resolve("velocity.toml")));
  }
}
