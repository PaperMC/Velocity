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

import com.electronwill.nightconfig.core.UnmodifiableConfig;
import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.velocitypowered.proxy.config.ConfigurationDocument;
import com.velocitypowered.proxy.config.YamlDocument;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.logging.log4j.Logger;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Brings a legacy TOML configuration up to version 2.9 and ports it verbatim to YAML.
 */
public final class TomlToYamlConverter {

  /**
   * TOML 2.9 became YAML 3.
   */
  private static final int TARGET_VERSION = 3;
  private static final String BACKUP_SUFFIX = ".bak";

  private TomlToYamlConverter() {
  }

  /**
   * Converts the TOML configuration at {@code legacyPath} into a YAML configuration at
   * {@code targetPath}, renaming the legacy file once the conversion has been written.
   */
  public static void convert(final Path legacyPath, final Path targetPath, final Logger logger)
      throws IOException {
    logger.info("Converting {} to {}.", legacyPath, targetPath);

    final ConfigurationDocument document;
    try (CommentedFileConfig legacy = CommentedFileConfig.builder(legacyPath)
        .preserveInsertionOrder()
        .sync()
        .build()
    ) {
      legacy.load();

      final LegacyConfigurationMigration[] migrations = {
          new ForwardingMigration(),
          new KeyAuthenticationMigration(),
          new MotdMigration(),
          new MiniMessageTranslationsMigration(),
          new TransferIntegrationMigration(),
          new PacketLimiterMigration(),
          new PingPassthroughMigration(),
      };

      for (final LegacyConfigurationMigration migration : migrations) {
        if (migration.shouldMigrate(legacy)) {
          migration.migrate(legacy, logger);
        }
      }

      document = YamlDocument.of(port(legacy));
    }

    renameProxyProtocol(document);
    document.set("config-version", TARGET_VERSION);
    document.save(targetPath);

    final Path backupPath = legacyPath.resolveSibling(legacyPath.getFileName() + BACKUP_SUFFIX);
    Files.move(legacyPath, backupPath, StandardCopyOption.REPLACE_EXISTING);
    logger.info("Your configuration has been converted to YAML. The old configuration has been "
        + "kept at {}.", backupPath);
  }

  private static Map<String, Object> port(final UnmodifiableConfig config) {
    final Map<String, Object> ported = new LinkedHashMap<>();
    for (final UnmodifiableConfig.Entry entry : config.entrySet()) {
      ported.put(cleanKey(entry.getKey()), portValue(entry.getValue()));
    }
    return ported;
  }

  private static @Nullable Object portValue(final @Nullable Object value) {
    if (value instanceof UnmodifiableConfig section) {
      return port(section);
    }
    if (value instanceof List<?> list) {
      final List<Object> ported = new ArrayList<>(list.size());
      for (final Object element : list) {
        ported.add(portValue(element));
      }
      return ported;
    }
    return value;
  }

  private static void renameProxyProtocol(final ConfigurationDocument document) {
    if (!document.contains("advanced.proxy-protocol")) {
      return;
    }
    if (!document.contains("advanced.haproxy-protocol")) {
      document.set("advanced.haproxy-protocol", document.getBoolean("advanced.proxy-protocol"));
    }
    document.remove("advanced.proxy-protocol");
  }

  /**
   * A TOML key outside {@code [A-Za-z0-9_-]} must be quoted, and comes back with its quotes.
   */
  private static String cleanKey(final String key) {
    return key.replace("\"", "");
  }
}
