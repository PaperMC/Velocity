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

import static com.velocitypowered.proxy.config.migration.MiniMessageTranslationsMigration.migrateLegacyColors;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.io.IOException;
import java.io.StringReader;
import java.util.Properties;
import org.junit.jupiter.api.Test;

class MiniMessageTranslationsMigrationTest {

  @Test
  void leavesContentWithoutLegacyCodesUntouched() {
    final String content = "a=Hallo\\! <arg:0>\\: \\u00e9\\n\n";
    assertSame(content, migrateLegacyColors(content));
  }

  @Test
  void preservesPropertiesEscapes() throws IOException {
    final String migrated = migrateLegacyColors("a=§cHallo\\! \\u00e9\\nnext\\\\\n");
    assertEquals("a=<red>Hallo\\! \\u00e9\\nnext\\\\\n", migrated);
    assertEquals("<red>Hallo! é\nnext\\", load(migrated).getProperty("a"));
  }

  @Test
  void preservesKeysAndSeparators() throws IOException {
    final String migrated = migrateLegacyColors("key\\:with\\=sep  =  §avalue\n");
    assertEquals("key\\:with\\=sep  =  <green>value\n", migrated);
    assertEquals("<green>value", load(migrated).getProperty("key:with=sep"));
  }

  @Test
  void preservesExistingMiniMessageTags() {
    assertEquals("a=<arg:0> <red>red\n", migrateLegacyColors("a=<arg:0> §cred\n"));
  }

  @Test
  void leavesCommentsUntouched() {
    assertEquals("# §c <comment>\\\na=<red>x\n",
        migrateLegacyColors("# §c <comment>\\\na=§cx\n"));
  }

  @Test
  void formattingDoesNotBleedIntoNextEntry() throws IOException {
    final Properties properties = load(migrateLegacyColors("a=§cred\nb=plain\n"));
    assertEquals("<red>red", properties.getProperty("a"));
    assertEquals("plain", properties.getProperty("b"));
  }

  @Test
  void formattingContinuesAcrossLineContinuations() throws IOException {
    final String migrated = migrateLegacyColors("a=§cfoo \\\n    bar\nb=plain\n");
    assertEquals("a=<red>foo \\\n    bar\nb=plain\n", migrated);
    final Properties properties = load(migrated);
    assertEquals("<red>foo bar", properties.getProperty("a"));
    assertEquals("plain", properties.getProperty("b"));
  }

  @Test
  void preservesCrlfLineEndings() {
    assertEquals("a=<red>x\r\nb=y\r\n", migrateLegacyColors("a=§cx\r\nb=y\r\n"));
  }

  @Test
  void handlesMissingTrailingNewline() {
    assertEquals("a=<red>x", migrateLegacyColors("a=§cx"));
    assertEquals("a=<red>x\\", migrateLegacyColors("a=§cx\\"));
  }

  @Test
  void leavesInvalidCodesUntouched() {
    final String content = "a=§zfoo\\! <arg:0>\nb=trailing§\n";
    assertEquals(content, migrateLegacyColors(content));
  }

  @Test
  void isIdempotent() {
    final String once = migrateLegacyColors("a=§c§lHi\\! <arg:0>\\\n  more\nb=§zx\n");
    assertEquals(once, migrateLegacyColors(once));
  }

  private static Properties load(final String content) throws IOException {
    final Properties properties = new Properties();
    properties.load(new StringReader(content));
    return properties;
  }
}
