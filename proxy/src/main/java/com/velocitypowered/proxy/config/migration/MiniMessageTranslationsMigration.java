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

package com.velocitypowered.proxy.config.migration;

import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import java.io.IOException;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.apache.logging.log4j.Logger;

/**
 * Migration from old to modern language argument format with MiniMessage.
 * Also migrates possible use of legacy colors to MiniMessage format.
 */
public final class MiniMessageTranslationsMigration implements ConfigurationMigration {
  private final List<Path> skippedFiles = new ArrayList<>();

  @Override
  public boolean shouldMigrate(final CommentedFileConfig config) {
    // This migration does not bump the config version itself. It shipped before the 2.8 bump and
    // runs ahead of PacketLimiterMigration, so any config at 2.8 or later has already been through it.
    return configVersion(config) < 2.8;
  }

  @Override
  public void migrate(final CommentedFileConfig config, final Logger logger) throws IOException {
    final Path langFolder = Path.of("lang");
    if (Files.notExists(langFolder)) {
      return;
    }
    final Pattern oldPlaceholderPattern = Pattern.compile("\\{(\\d+)}");
    try (final DirectoryStream<Path> stream
                 = Files.newDirectoryStream(langFolder, MiniMessageTranslationsMigration::isTranslationFile)) {
      for (final Path path : stream) {
        String content;
        try {
          content = Files.readString(path, StandardCharsets.UTF_8);
        } catch (final CharacterCodingException e) {
          logger.warn("Skipping migration of translation file {} as it is not valid UTF-8.", path);
          skippedFiles.add(path);
          continue;
        }
        // Some setups use legacy color codes, this format is migrated to MiniMessage
        String migrated = migrateLegacyColors(content);
        // Migrate old arguments
        migrated = oldPlaceholderPattern.matcher(migrated).replaceAll("<arg:$1>");
        if (!migrated.equals(content)) {
          Files.writeString(path, migrated, StandardCharsets.UTF_8);
        }
      }
    }
  }

  /**
   * Halts startup if any translation file had to be skipped. This migration only runs once, so a
   * skipped file is never migrated automatically and the user has to convert it by hand.
   *
   * @throws ConfigurationMigrationHaltException if any translation file was skipped
   */
  public void haltIfFilesSkipped() throws ConfigurationMigrationHaltException {
    if (skippedFiles.isEmpty()) {
      return;
    }
    throw new ConfigurationMigrationHaltException("The following translation files are not valid "
        + "UTF-8 and could not be migrated to the MiniMessage format: " + skippedFiles + ". "
        + "Your velocity.toml has been migrated, and these files will not be migrated "
        + "automatically. Re-save them as UTF-8, replace {0}-style arguments with <arg:0> and "
        + "legacy color codes with MiniMessage tags, or delete them. Then start Velocity again.");
  }

  /**
   * Converts legacy color codes in the values of a properties file to MiniMessage tags. Keys,
   * comments and properties escape sequences are left untouched, and each value is converted on
   * its own so formatting does not bleed from one entry into the next.
   *
   * @param content the raw properties file content
   * @return the content with legacy color codes converted
   */
  static String migrateLegacyColors(final String content) {
    if (content.indexOf(LegacyComponentSerializer.SECTION_CHAR) == -1) {
      return content;
    }
    final StringBuilder out = new StringBuilder(content.length());
    // The value of the current logical line, which spans several physical lines when continued
    final StringBuilder value = new StringBuilder();
    boolean continuation = false;
    for (final String line : content.split("(?<=\n)")) {
      int bodyEnd = line.length();
      if (bodyEnd > 0 && line.charAt(bodyEnd - 1) == '\n') {
        bodyEnd--;
      }
      if (bodyEnd > 0 && line.charAt(bodyEnd - 1) == '\r') {
        bodyEnd--;
      }
      final String body = line.substring(0, bodyEnd);
      if (!continuation) {
        if (isBlankOrComment(body)) {
          out.append(line);
          continue;
        }
        final int valueStart = valueStart(body);
        out.append(body, 0, valueStart);
        value.append(body, valueStart, body.length());
      } else {
        value.append(body);
      }
      continuation = endsWithContinuation(body);
      if (continuation) {
        value.append(line, bodyEnd, line.length());
        continue;
      }
      out.append(legacyToMiniMessage(value.toString())).append(line, bodyEnd, line.length());
      value.setLength(0);
    }
    // The file ended while a value was still being continued
    out.append(legacyToMiniMessage(value.toString()));
    return out.toString();
  }

  private static String legacyToMiniMessage(final String value) {
    if (value.indexOf(LegacyComponentSerializer.SECTION_CHAR) == -1) {
      return value;
    }
    final String serialized = MiniMessage.miniMessage().serialize(
        LegacyComponentSerializer.legacySection().deserialize(value));
    // The serializer escapes '\' and '<' in text. The value is still in its raw properties form,
    // so a '\' is a properties escape and any '<' is already MiniMessage, and both must be kept
    // as they were. The tags emitted for legacy codes contain neither, so undoing every escape
    // the serializer added restores the original text exactly.
    final StringBuilder unescaped = new StringBuilder(serialized.length());
    for (int i = 0; i < serialized.length(); i++) {
      final char c = serialized.charAt(i);
      if (c == '\\' && i + 1 < serialized.length()) {
        unescaped.append(serialized.charAt(++i));
      } else {
        unescaped.append(c);
      }
    }
    return unescaped.toString();
  }

  private static boolean isBlankOrComment(final String line) {
    for (int i = 0; i < line.length(); i++) {
      final char c = line.charAt(i);
      if (!isPropertiesWhitespace(c)) {
        return c == '#' || c == '!';
      }
    }
    return true;
  }

  /**
   * Finds where the value starts in the first physical line of a properties entry, following the
   * key and separator rules of {@link java.util.Properties#load(java.io.Reader)}.
   */
  private static int valueStart(final String line) {
    final int length = line.length();
    int i = 0;
    while (i < length && isPropertiesWhitespace(line.charAt(i))) {
      i++;
    }
    while (i < length) {
      final char c = line.charAt(i);
      if (c == '\\') {
        i += 2;
        continue;
      }
      if (c == '=' || c == ':' || isPropertiesWhitespace(c)) {
        break;
      }
      i++;
    }
    while (i < length && isPropertiesWhitespace(line.charAt(i))) {
      i++;
    }
    if (i < length && (line.charAt(i) == '=' || line.charAt(i) == ':')) {
      i++;
      while (i < length && isPropertiesWhitespace(line.charAt(i))) {
        i++;
      }
    }
    return Math.min(i, length);
  }

  private static boolean endsWithContinuation(final String line) {
    int backslashes = 0;
    for (int i = line.length() - 1; i >= 0 && line.charAt(i) == '\\'; i--) {
      backslashes++;
    }
    return backslashes % 2 == 1;
  }

  private static boolean isPropertiesWhitespace(final char c) {
    return c == ' ' || c == '\t' || c == '\f';
  }

  private static boolean isTranslationFile(final Path path) {
    return Files.isRegularFile(path) && path.getFileName().toString().endsWith(".properties");
  }
}
