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
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.apache.logging.log4j.Logger;

/**
 * Migration from old to modern language argument format with MiniMessage.
 * Also migrates possible use of legacy colors to MiniMessage format.
 */
public final class MiniMessageTranslationsMigration implements ConfigurationMigration {
  @Override
  public boolean shouldMigrate(final CommentedFileConfig config) {
    // Checking whether translations should be migrated would be just as costly as attempting to migrate them directly.
    return true;
  }

  @Override
  public void migrate(final CommentedFileConfig config, final Logger logger) throws IOException {
    final Path langFolder = Path.of("lang");
    if (Files.notExists(langFolder)) {
      return;
    }
    final Pattern oldPlaceholderPattern = Pattern.compile("\\{(\\d+)}");
    try (final DirectoryStream<Path> stream
                 = Files.newDirectoryStream(langFolder, Files::isRegularFile)) {
      for (final Path path : stream) {
        String content = Files.readString(path, StandardCharsets.UTF_8);
        if (content.indexOf('{') == -1) {
          continue;
        }
        // Migrate old arguments
        content = oldPlaceholderPattern.matcher(content).replaceAll("<arg:$1>");
        // Some setups use legacy color codes, this format is migrated to MiniMessage
        content = MiniMessage.miniMessage().serialize(
                LegacyComponentSerializer.legacySection().deserialize(content));
        Files.writeString(path, content, StandardCharsets.UTF_8);
      }
    }
  }
}
