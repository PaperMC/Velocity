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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class YamlDocumentTest {

  private enum Mode {
    NONE,
    BUNGEE_GUARD
  }

  private static final String SAMPLE = """
      # Config version. Do not change this
      config-version: 3

      # What port should the proxy be bound to?
      bind: "0.0.0.0:25565"

      # Configure your servers here.
      servers:
        lobby: "127.0.0.1:30066"

        # In what order we should try servers.
        try:
          - lobby

      # Advanced options.
      advanced:
        # How large a packet has to be before we compress it.
        compression-threshold: 256
        compression-level: -1
      """;

  private static YamlDocument sample() throws IOException {
    return YamlDocument.read(new StringReader(SAMPLE));
  }

  private static String emit(YamlDocument document) throws IOException {
    final StringWriter writer = new StringWriter();
    document.write(writer);
    return writer.toString();
  }

  @Test
  void readsScalarsAsTheTypeTheyAreAskedFor() throws IOException {
    final YamlDocument document = sample();
    assertEquals(3, document.getInt("config-version"));
    assertEquals("0.0.0.0:25565", document.getString("bind"));
    assertEquals(256, document.getInt("advanced.compression-threshold"));
    assertEquals(-1, document.getInt("advanced.compression-level"));
    assertEquals("127.0.0.1:30066", document.getString("servers.lobby"));
    assertEquals(List.of("lobby"), document.getStringList("servers.try"));
  }

  @Test
  void readsOneValueAsListOfOne() throws IOException {
    assertEquals(List.of("127.0.0.1:30066"), sample().getStringList("servers.lobby"));
  }

  @Test
  void readsEnumsHoweverTheirNameIsWritten() throws IOException {
    final YamlDocument document = YamlDocument.read(new StringReader("""
        written-plainly: BUNGEE_GUARD
        written-loosely: bungee_guard
        written-wrongly: nowhere
        """));

    assertEquals(Mode.BUNGEE_GUARD, document.getEnum("written-plainly", Mode.class));
    assertEquals(Mode.BUNGEE_GUARD, document.getEnum("written-loosely", Mode.class));
    assertThrows(IllegalArgumentException.class,
        () -> document.getEnum("written-wrongly", Mode.class));
  }

  @Test
  void containsNoUnknownPaths() throws IOException {
    final YamlDocument document = sample();
    assertFalse(document.contains("nope"));
    assertFalse(document.contains("advanced.nope"));
    assertFalse(document.contains("nope.nope"));
    assertFalse(document.contains("bind.nope"));
    assertTrue(document.contains("advanced.compression-level"));
    assertThrows(IllegalStateException.class, () -> document.getString("advanced.nope"));
  }

  @Test
  void preservesCommentsAndBlankLinesAcrossAnUntouchedRoundTrip() throws IOException {
    assertEquals(SAMPLE, emit(sample()));
  }

  @Test
  void preservesCommentsOfKeysItRewrites() throws IOException {
    final YamlDocument document = sample();
    document.set("advanced.compression-threshold", 512);
    document.set("config-version", 4);

    final String emitted = emit(document);
    assertTrue(emitted.contains("# How large a packet has to be before we compress it.\n"
        + "  compression-threshold: 512"), emitted);
    assertTrue(emitted.contains("# Config version. Do not change this\nconfig-version: 4"),
        emitted);
  }

  @Test
  void writesNewValuesWithComments() throws IOException {
    final YamlDocument document = sample();
    document.set("advanced.brand-new", true);
    document.setComment("advanced.brand-new", "A brand new option\non two lines");

    assertTrue(document.getBoolean("advanced.brand-new"));
    assertEquals("A brand new option\non two lines", document.getComment("advanced.brand-new"));
    assertTrue(emit(document).contains("\n  # A brand new option\n  # on two lines\n"
        + "  brand-new: true"), emit(document));
  }

  @Test
  void createsMissingSectionsWhenWriting() throws IOException {
    final YamlDocument document = sample();
    document.set("brand.new.section", "value");
    assertEquals("value", document.getString("brand.new.section"));
    assertEquals(Set.of("section"), document.getSection("brand.new").keys());
  }

  @Test
  void removesValuesAndTheirComments() throws IOException {
    final YamlDocument document = sample();
    document.remove("advanced.compression-threshold");

    assertFalse(document.contains("advanced.compression-threshold"));
    final String emitted = emit(document);
    assertFalse(emitted.contains("compression-threshold"), emitted);
    assertFalse(emitted.contains("How large a packet"), emitted);
  }

  @Test
  void quotesStringsThatWouldOtherwiseChangeType() throws IOException {
    final YamlDocument document = sample();
    document.set("bind", "yes");
    assertEquals("yes", YamlDocument.read(new StringReader(emit(document))).getString("bind"));
  }

  @Test
  void writesTheSameDocumentEveryTimeItIsRewritten() throws IOException {
    final String once = emit(sample());
    assertEquals(once, emit(YamlDocument.read(new StringReader(once))));
  }

  @Test
  void listsEveryPathAndEverySection() throws IOException {
    assertEquals(List.of("config-version", "bind", "servers", "servers.lobby", "servers.try",
            "advanced", "advanced.compression-threshold", "advanced.compression-level"),
        List.copyOf(sample().paths()));
  }

  @Test
  void listsEveryLeafPath() throws IOException {
    assertEquals(List.of("config-version", "bind", "servers.lobby", "servers.try",
            "advanced.compression-threshold", "advanced.compression-level"),
        List.copyOf(sample().leafPaths()));
  }

  @Test
  void copiesCommentsOntoSharedKeysOnly() throws IOException {
    final YamlDocument target = YamlDocument.read(new StringReader("""
        config-version: 3
        bind: "1.2.3.4:25577"
        unknown-option: true
        servers:
          alpha: "1.2.3.4:30066"
          try:
            - alpha
        advanced:
          compression-threshold: 512
        """));
    target.copyCommentsFrom(sample());

    assertEquals("What port should the proxy be bound to?", target.getComment("bind"));
    assertEquals("In what order we should try servers.", target.getComment("servers.try"));
    assertEquals("How large a packet has to be before we compress it.",
        target.getComment("advanced.compression-threshold"));
    assertNull(target.getComment("unknown-option"));
    assertNull(target.getComment("servers.alpha"));

    final YamlDocument reloaded = YamlDocument.read(new StringReader(emit(target)));
    assertEquals("1.2.3.4:25577", reloaded.getString("bind"));
    assertEquals(512, reloaded.getInt("advanced.compression-threshold"));
    assertEquals(Set.of("alpha", "try"), reloaded.getSection("servers").keys());
    assertEquals("1.2.3.4:30066", reloaded.getString("servers.alpha"));
    assertEquals(List.of("alpha"), reloaded.getStringList("servers.try"));
  }

  @Test
  void copiesNoCommentsWhenTheKeyIsAbsentFromTheSource() throws IOException {
    final YamlDocument target = sample();
    target.copyCommentsFrom(YamlDocument.read(new StringReader("unrelated: true\n")));
    assertEquals("Config version. Do not change this", target.getComment("config-version"));
  }

  @Test
  void readsSectionEntriesWhoseKeysContainDots() throws IOException {
    final Configuration hosts = YamlDocument.read(new StringReader("""
        forced-hosts:
          "lobby.example.com":
            - lobby
            - lobby-two
          "shop.example.com": shop
        """)).getSection("forced-hosts");

    assertEquals(Set.of("lobby.example.com", "shop.example.com"), hosts.keys());
    assertTrue(hosts.contains("lobby.example.com"));
    assertEquals(List.of("lobby", "lobby-two"), hosts.getStringList("lobby.example.com"));
    assertEquals(List.of("shop"), hosts.getStringList("shop.example.com"));
    assertEquals("shop", hosts.getString("shop.example.com"));
  }

  @Test
  void readsNestedSectionsByPathFromTheRoot() throws IOException {
    final YamlDocument document = sample();
    assertEquals(Set.of("config-version", "bind", "servers", "advanced"), document.keys());
    assertEquals(Set.of("compression-threshold", "compression-level"),
        document.getSection("advanced").keys());
    assertEquals(256, document.getSection("advanced").getInt("compression-threshold"));
    assertEquals(256, document.getInt("advanced.compression-threshold"));
  }

  @Test
  void rejectsValuesOfTheWrongShape() throws IOException {
    final YamlDocument document = sample();
    assertThrows(IllegalArgumentException.class, () -> document.getString("advanced"));
    assertThrows(IllegalArgumentException.class, () -> document.getInt("bind"));
    assertThrows(IllegalArgumentException.class, () -> document.getBoolean("bind"));
    assertThrows(IllegalArgumentException.class, () -> document.getStringList("advanced"));
    assertThrows(IllegalStateException.class, () -> document.getSection("nope"));
    assertThrows(IllegalArgumentException.class, () -> document.getSection("bind"));
  }

  @Test
  void savesAndLoadsFromDisk(@TempDir Path directory) throws IOException {
    final Path path = directory.resolve("config.yml");
    sample().save(path);
    assertEquals(SAMPLE, Files.readString(path, StandardCharsets.UTF_8));
    assertEquals(3, YamlDocument.load(path).getInt("config-version"));
  }
}
