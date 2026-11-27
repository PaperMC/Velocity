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

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;

class VelocityConfigurationTest {

  private static VelocityConfiguration readIn(Path directory) throws IOException {
    return VelocityConfiguration.read(directory.resolve("velocity.yaml"));
  }

  private static void writeConfiguration(Path directory, String contents) throws IOException {
    Files.writeString(directory.resolve("velocity.yaml"), contents, StandardCharsets.UTF_8);
  }

  private static String shippedConfiguration() throws IOException {
    try (InputStream stream = VelocityConfiguration.class.getClassLoader()
        .getResourceAsStream("default-velocity.yaml")) {
      return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
    }
  }

  @Test
  void writesTheShippedConfigurationVerbatimOnFirstStart(@TempDir Path directory)
      throws IOException {
    readIn(directory);

    assertEquals(shippedConfiguration(),
        Files.readString(directory.resolve("velocity.yaml"), StandardCharsets.UTF_8));
    assertTrue(Files.exists(directory.resolve("forwarding.secret")));
  }

  @Test
  void fillsAnEmptyFileInPlace(@TempDir Path directory) throws IOException {
    // What a Docker user mounting a fresh, empty velocity.yaml starts with.
    writeConfiguration(directory, "");
    readIn(directory);

    assertEquals(shippedConfiguration(),
        Files.readString(directory.resolve("velocity.yaml"), StandardCharsets.UTF_8));
  }

  @Test
  void explainsWhenDirectoryStandsInForTheFile(@TempDir Path directory) throws IOException {
    Files.createDirectory(directory.resolve("velocity.yaml"));

    final IOException thrown = assertThrows(IOException.class, () -> readIn(directory));
    assertTrue(thrown.getMessage().contains("is a directory"), thrown.getMessage());
  }

  @Test
  void rewritesTheShippedConfigurationWithoutChangingIt() throws IOException {
    final StringWriter rewritten = new StringWriter();
    VelocityConfiguration.yamlLoader().sink(() -> new BufferedWriter(rewritten)).build()
        .save(VelocityConfiguration.loadDefaults());

    assertEquals(shippedConfiguration(), rewritten.toString(),
        "default-velocity.yaml is not laid out the way Velocity writes YAML, so a converted "
            + "configuration would look different from a fresh one. Match the emitted form.");
  }

  @Test
  void appliesTheShippedDefaults(@TempDir Path directory) throws IOException {
    final VelocityConfiguration configuration = readIn(directory);

    assertEquals(25565, configuration.getBind().getPort());
    assertEquals(500, configuration.getShowMaxPlayers());
    assertTrue(configuration.isOnlineMode());
    assertEquals(256, configuration.getCompressionThreshold());
    assertEquals(-1, configuration.getCompressionLevel());
    assertEquals(PlayerInfoForwarding.NONE, configuration.getPlayerInfoForwardingMode());
    assertEquals(new VelocityConfiguration.PacketLimiterConfig(7, -1, -1, 5242880),
        configuration.getPacketLimiterConfig());
    assertTrue(configuration.getMetrics().isEnabled());
    assertEquals(List.of("lobby"), configuration.getAttemptConnectionOrder());
    assertEquals(3, configuration.getServers().size());
    assertEquals(3, configuration.getForcedHosts().size());

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
  void takesUserNamedSectionsWholeRatherThanMergingThem(@TempDir Path directory)
      throws IOException {
    writeConfiguration(directory, """
        config-version: 3
        servers:
          alpha: "127.0.0.1:30066"
        try:
          - alpha
        forced-hosts:
          "Alpha.Example.com": alpha
        """);

    final VelocityConfiguration configuration = readIn(directory);
    assertEquals(Map.of("alpha", "127.0.0.1:30066"), configuration.getServers());
    assertEquals(List.of("alpha"), configuration.getAttemptConnectionOrder());
    assertEquals(Map.of("alpha.example.com", List.of("alpha")), configuration.getForcedHosts());
  }

  @Test
  void allowsServerNamedTry(@TempDir Path directory) throws IOException {
    writeConfiguration(directory, """
        config-version: 3
        servers:
          try: "127.0.0.1:30066"
        try:
          - try
        forced-hosts: {}
        """);

    final VelocityConfiguration configuration = readIn(directory);
    assertEquals(Map.of("try", "127.0.0.1:30066"), configuration.getServers());
    assertEquals(List.of("try"), configuration.getAttemptConnectionOrder());
  }

  @Test
  void anEmptyUserNamedSectionStaysEmpty(@TempDir Path directory) throws IOException {
    writeConfiguration(directory, """
        config-version: 3
        forced-hosts: {}
        """);

    assertEquals(Map.of(), readIn(directory).getForcedHosts());
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
        Files.readString(directory.resolve("velocity.yaml"), StandardCharsets.UTF_8));
  }

  @Test
  void refusesConfigurationsFromOtherVersions(@TempDir Path directory) throws IOException {
    writeConfiguration(directory, "config-version: 4\n");

    final IllegalStateException thrown =
        assertThrows(IllegalStateException.class, () -> readIn(directory));
    assertTrue(thrown.getMessage().contains("only understands config-version 3"),
        thrown.getMessage());
  }

  @Test
  void refusesConfigurationsThatDeclareNoVersion(@TempDir Path directory) throws IOException {
    writeConfiguration(directory, "motd: \"No version here\"\n");

    final IllegalStateException thrown =
        assertThrows(IllegalStateException.class, () -> readIn(directory));
    assertTrue(thrown.getMessage().contains("has no config-version"), thrown.getMessage());
  }

  @Test
  void refusesFractionalVersions(@TempDir Path directory) throws IOException {
    writeConfiguration(directory, "config-version: \"2.9\"\n");

    final SerializationException thrown =
        assertThrows(SerializationException.class, () -> readIn(directory));
    assertTrue(thrown.getMessage().contains("[config-version]"), thrown.getMessage());
  }

  @Test
  void namesTheOptionThatHasTheWrongType(@TempDir Path directory) throws IOException {
    writeConfiguration(directory, """
        config-version: 3
        advanced:
          compression-threshold: lots
        """);

    final SerializationException thrown =
        assertThrows(SerializationException.class, () -> readIn(directory));
    assertTrue(thrown.getMessage().contains("[advanced, compression-threshold]"),
        thrown.getMessage());
  }

  @Test
  void namesAnUnknownEnumValue(@TempDir Path directory) throws IOException {
    writeConfiguration(directory, """
        config-version: 3
        forwarding:
          mode: sideways
        """);

    final SerializationException thrown =
        assertThrows(SerializationException.class, () -> readIn(directory));
    assertTrue(thrown.getMessage().contains("[forwarding, mode]"), thrown.getMessage());
  }

  @Test
  void readsEveryShippedOption(@TempDir Path directory) throws IOException {
    final CommentedConfigurationNode defaults = VelocityConfiguration.loadDefaults();
    final ConfigurationNode empty = VelocityConfiguration.yamlLoader().build().createNode();
    final Path path = directory.resolve("velocity.yaml");

    // Every option read has a default: reading the defaults alone works.
    VelocityConfiguration.read(VelocityConfiguration.withDefaults(empty, defaults), path);

    // And every default is read: without it, reading fails.
    final List<String> unread = new ArrayList<>();
    for (final List<Object> option : leafPaths(defaults, List.of())) {
      if (option.equals(List.of("config-version"))
          || VelocityConfiguration.USER_NAMED_SECTIONS.contains(String.valueOf(option.get(0)))) {
        continue;
      }
      final CommentedConfigurationNode without = defaults.copy();
      without.node(option.toArray()).raw(null);
      try {
        VelocityConfiguration.read(VelocityConfiguration.withDefaults(empty, without), path);
        unread.add(String.join(".", option.stream().map(String::valueOf).toList()));
      } catch (final NoSuchElementException expected) {
        // The option was read.
      }
    }
    assertEquals(List.of(), unread, "default-velocity.yaml defines options nothing reads");
  }

  private static List<List<Object>> leafPaths(ConfigurationNode node, List<Object> prefix) {
    final List<List<Object>> paths = new ArrayList<>();
    for (final Map.Entry<Object, ? extends ConfigurationNode> child
        : node.childrenMap().entrySet()) {
      final List<Object> path = new ArrayList<>(prefix);
      path.add(child.getKey());
      if (child.getValue().isMap()) {
        paths.addAll(leafPaths(child.getValue(), path));
      } else {
        paths.add(path);
      }
    }
    return paths;
  }
}
