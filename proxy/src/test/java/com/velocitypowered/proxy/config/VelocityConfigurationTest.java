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

package com.velocitypowered.proxy.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.velocitypowered.proxy.config.migration.ConfigurationMigration;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class VelocityConfigurationTest {

  private static final Set<String> DYNAMIC_SECTIONS = Set.of("servers", "forced-hosts");

  private static VelocityConfiguration readIn(Path directory) throws IOException {
    return VelocityConfiguration.read(directory.resolve("velocity.yml"));
  }

  private static void writeConfiguration(Path directory, String contents) throws IOException {
    Files.writeString(directory.resolve("velocity.yml"), contents, StandardCharsets.UTF_8);
  }

  private static String shippedConfiguration() throws IOException {
    try (InputStream stream = VelocityConfiguration.class.getClassLoader()
        .getResourceAsStream("default-velocity.yml")) {
      return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
    }
  }

  @Test
  void writesTheShippedConfigurationVerbatimOnFirstStart(@TempDir Path directory)
      throws IOException {
    readIn(directory);

    assertEquals(shippedConfiguration(),
        Files.readString(directory.resolve("velocity.yml"), StandardCharsets.UTF_8));
    assertTrue(Files.exists(directory.resolve("forwarding.secret")));
  }

  @Test
  void rewritesTheShippedConfigurationWithoutChangingIt() throws IOException {
    final StringWriter rewritten = new StringWriter();
    VelocityConfiguration.defaultConfiguration().write(rewritten);

    assertEquals(shippedConfiguration(), rewritten.toString(),
        "default-velocity.yml is not written the way YamlDocument writes YAML, so the first "
            + "migration that saves a configuration would reformat the whole file. Match the "
            + "emitted form above: two-space indent, indented list items, double quotes, and a "
            + "single blank line between options.");
  }

  @Test
  void formatsTheShippedConfigurationConsistently() throws IOException {
    final String shipped = shippedConfiguration();
    final String[] lines = shipped.split("\n", -1);
    for (int i = 1; i < lines.length; i++) {
      assertFalse(lines[i].isEmpty() && lines[i - 1].isEmpty(),
          "default-velocity.yml has consecutive blank lines at line " + (i + 1)
              + "; options are separated by a single blank line. Rewriting the file preserves "
              + "blank lines, so nothing else catches this.");
    }
    assertTrue(shipped.endsWith("\n") && !shipped.endsWith("\n\n"),
        "default-velocity.yml must end with exactly one newline");
  }

  @Test
  void appliesTheShippedDefaults(@TempDir Path directory) throws IOException {
    final VelocityConfiguration configuration = readIn(directory);

    assertEquals("0.0.0.0:25565", configuration.getBind().getHostString() + ":"
        + configuration.getBind().getPort());
    assertEquals(500, configuration.getShowMaxPlayers());
    assertTrue(configuration.isOnlineMode());
    assertEquals(256, configuration.getCompressionThreshold());
    assertEquals(-1, configuration.getCompressionLevel());
    assertEquals(PlayerInfoForwarding.NONE, configuration.getPlayerInfoForwardingMode());
    assertEquals(new VelocityConfiguration.PacketLimiterConfig(7, -1, -1, 5242880),
        configuration.getPacketLimiterConfig());
    assertTrue(configuration.getMetrics().isEnabled());

    assertFalse(configuration.isAnnounceForge());
    assertEquals(50, configuration.getCommandRatelimit());
    assertEquals(0, configuration.getKickAfterRateLimitedCommands());
    assertEquals(10, configuration.getTabCompleteRatelimit());
    assertEquals(0, configuration.getKickAfterRateLimitedTabCompletes());
  }

  @Test
  void takesUnsetOptionsFromTheDefaults(@TempDir Path directory) throws IOException {
    writeConfiguration(directory, """
        config-version: 3
        motd: "Overridden"
        advanced:
          compression-threshold: 512
        """);

    final VelocityConfiguration configuration = readIn(directory);
    assertEquals(512, configuration.getCompressionThreshold());
    assertEquals(-1, configuration.getCompressionLevel());
    assertEquals(30000, configuration.getReadTimeout());
    assertEquals(500, configuration.getShowMaxPlayers());
  }

  @Test
  void takesDynamicSectionsWholeRatherThanMergingThem(@TempDir Path directory) throws IOException {
    writeConfiguration(directory, """
        config-version: 3
        servers:
          alpha: "127.0.0.1:30066"
          try:
            - alpha
        forced-hosts:
          "alpha.example.com":
            - alpha
        """);

    final VelocityConfiguration configuration = readIn(directory);
    assertEquals(Map.of("alpha", "127.0.0.1:30066"), configuration.getServers());
    assertEquals(List.of("alpha"), configuration.getAttemptConnectionOrder());
    assertEquals(Map.of("alpha.example.com", List.of("alpha")), configuration.getForcedHosts());
  }

  @Test
  void leavesAnUpToDateConfigurationUntouched(@TempDir Path directory) throws IOException {
    final String written = """
        config-version: 3
        motd: "Mine, uncommented, and staying that way"
        """;
    writeConfiguration(directory, written);
    readIn(directory);

    assertEquals(written,
        Files.readString(directory.resolve("velocity.yml"), StandardCharsets.UTF_8));
  }

  @Test
  void refusesConfigurationsFromNewerVersions(@TempDir Path directory) throws IOException {
    final int current = ConfigurationMigration.versionOf(
        VelocityConfiguration.defaultConfiguration());
    writeConfiguration(directory, "config-version: " + (current + 1) + "\n");

    final IllegalStateException thrown =
        assertThrows(IllegalStateException.class, () -> readIn(directory));
    assertTrue(thrown.getMessage().contains("only understands up to version"),
        thrown.getMessage());
  }

  @Test
  void refusesConfigurationsThatDeclareNoVersion(@TempDir Path directory) throws IOException {
    writeConfiguration(directory, "motd: \"No version here\"\n");

    final IllegalStateException thrown =
        assertThrows(IllegalStateException.class, () -> readIn(directory));
    assertTrue(thrown.getMessage().contains("does not declare a config-version"),
        thrown.getMessage());
  }

  @Test
  void refusesConfigurationsWithFractionalVersions(@TempDir Path directory)
      throws IOException {
    writeConfiguration(directory, "config-version: \"2.9\"\n");

    final IllegalStateException thrown =
        assertThrows(IllegalStateException.class, () -> readIn(directory));
    assertTrue(thrown.getMessage().contains("is not a whole number"), thrown.getMessage());
  }

  @Test
  void shipsDefaultsForEveryOptionThatIsRead(@TempDir Path directory) throws IOException {
    final YamlDocument defaults = VelocityConfiguration.defaultConfiguration();
    final Set<String> read = new LinkedHashSet<>();

    // Loading against the defaults alone throws if an option has no default.
    VelocityConfiguration.load(new LayeredConfiguration(new RecordingConfiguration(defaults, read)),
        directory.resolve("velocity.yml"));

    final Set<String> unread = new LinkedHashSet<>();
    for (final String path : defaults.leafPaths()) {
      final int separator = path.indexOf('.');
      if (separator != -1 && DYNAMIC_SECTIONS.contains(path.substring(0, separator))) {
        continue;
      }
      if (!path.equals("config-version") && !read.contains(path)) {
        unread.add(path);
      }
    }
    assertEquals(Set.of(), unread, "default-velocity.yml defines options nothing reads");

    assertEquals(new HashSet<>(), difference(read, allowedPaths(defaults)),
        "options are read that default-velocity.yml does not define");
  }

  private record RecordingConfiguration(Configuration delegate, Set<String> read)
      implements Configuration {

    @Override
    public boolean contains(String path) {
      read.add(path);
      return delegate.contains(path);
    }

    @Override
    public String getString(String path) {
      return delegate.getString(path);
    }

    @Override
    public int getInt(String path) {
      return delegate.getInt(path);
    }

    @Override
    public boolean getBoolean(String path) {
      return delegate.getBoolean(path);
    }

    @Override
    public List<String> getStringList(String path) {
      return delegate.getStringList(path);
    }

    @Override
    public <T extends Enum<T>> T getEnum(String path, Class<T> type) {
      return delegate.getEnum(path, type);
    }

    @Override
    public ConfigurationSection getSection(String path) {
      return delegate.getSection(path);
    }

    @Override
    public Set<String> keys() {
      return delegate.keys();
    }
  }

  private static Set<String> allowedPaths(YamlDocument defaults) {
    final Set<String> allowed = new HashSet<>(defaults.leafPaths());
    allowed.addAll(DYNAMIC_SECTIONS);
    return allowed;
  }

  private static Set<String> difference(Set<String> from, Set<String> remove) {
    final Set<String> difference = new HashSet<>(from);
    difference.removeAll(remove);
    return difference;
  }
}
