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

import static org.spongepowered.configurate.NodePath.path;

import com.electronwill.nightconfig.core.UnmodifiableConfig;
import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.velocitypowered.proxy.config.VelocityConfiguration;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.logging.log4j.Logger;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.transformation.ConfigurationTransformation;
import org.spongepowered.configurate.transformation.TransformAction;
import org.spongepowered.configurate.yaml.ScalarStyle;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

/**
 * Converts a legacy {@code velocity.toml} into {@code velocity.yaml}. The legacy migrations
 * first bring the TOML up to its last version, 2.9, which is then ported key for key and
 * stamped as YAML config-version 3.
 */
public final class TomlToYamlConverter {

  private static final String BACKUP_SUFFIX = ".bak";

  /**
   * Moves the options that were reorganised when the configuration became YAML.
   */
  private static final ConfigurationTransformation RESTRUCTURE = ConfigurationTransformation
      .builder()
      // A list among the server addresses kept anyone from naming a server "try".
      .addAction(path("servers", "try"), moveTo("try"))
      .addAction(path("player-info-forwarding-mode"), moveTo("forwarding", "mode"))
      .addAction(path("forwarding-secret-file"), moveTo("forwarding", "secret-file"))
      .addAction(path("advanced", "login-ratelimit"), moveTo("rate-limits", "login"))
      .addAction(path("advanced", "command-rate-limit"), moveTo("rate-limits", "commands"))
      .addAction(path("advanced", "forward-commands-if-rate-limited"),
          moveTo("rate-limits", "forward-commands-if-limited"))
      .addAction(path("advanced", "kick-after-rate-limited-commands"),
          moveTo("rate-limits", "kick-after-commands"))
      .addAction(path("advanced", "tab-complete-rate-limit"),
          moveTo("rate-limits", "tab-completes"))
      .addAction(path("advanced", "kick-after-rate-limited-tab-completes"),
          moveTo("rate-limits", "kick-after-tab-completes"))
      .addAction(path("enable-player-address-logging"), moveTo("logging", "player-addresses"))
      .addAction(path("advanced", "log-player-connections"),
          moveTo("logging", "player-connections"))
      .addAction(path("advanced", "log-command-executions"), moveTo("logging", "commands"))
      .addAction(path("advanced", "show-ping-requests"), moveTo("logging", "ping-requests"))
      .build();

  private TomlToYamlConverter() {
  }

  /**
   * Converts the TOML configuration at {@code legacyPath} into {@code targetPath}, then keeps the
   * TOML as a backup beside it.
   *
   * @param legacyPath the legacy TOML configuration
   * @param target the loader of the YAML configuration to write
   * @param defaults the shipped defaults, whose comments the converted file gets
   * @param logger the logger to report to
   * @throws IOException if either file could not be read or written
   */
  public static void convert(final Path legacyPath, final YamlConfigurationLoader target,
      final CommentedConfigurationNode defaults, final Logger logger) throws IOException {
    logger.info("Converting {} to YAML.", legacyPath.getFileName());

    final CommentedConfigurationNode converted = target.createNode();
    try (CommentedFileConfig legacy = CommentedFileConfig.builder(legacyPath)
        .preserveInsertionOrder()
        .sync()
        .build()
    ) {
      legacy.load();

      final ConfigurationMigration[] migrations = {
          new ForwardingMigration(),
          new KeyAuthenticationMigration(),
          new MotdMigration(),
          new MiniMessageTranslationsMigration(),
          new TransferIntegrationMigration(),
          new PacketLimiterMigration(),
          new PingPassthroughMigration(),
      };
      for (final ConfigurationMigration migration : migrations) {
        if (migration.shouldMigrate(legacy)) {
          migration.migrate(legacy, logger);
        }
      }

      port(legacy, converted);
      keepLegacyFallbacks(legacy, converted);
    }

    renameProxyProtocol(converted);
    RESTRUCTURE.apply(converted);
    converted.node("config-version")
        .hint(YamlConfigurationLoader.SCALAR_STYLE, null)
        .set(VelocityConfiguration.CONFIG_VERSION);
    orderLike(defaults, converted);
    copyComments(defaults, converted);
    target.save(converted);

    backUp(legacyPath, logger);
  }

  /**
   * Copies every value of {@code config} into {@code node}, in the same order. TOML strings are
   * always quoted, so they stay double-quoted in YAML.
   */
  private static void port(final UnmodifiableConfig config, final ConfigurationNode node)
      throws SerializationException {
    for (final UnmodifiableConfig.Entry entry : config.entrySet()) {
      portValue(entry.getValue(), node.node(cleanKey(entry.getKey())));
    }
  }

  private static void portValue(final @Nullable Object value, final ConfigurationNode node)
      throws SerializationException {
    if (value instanceof UnmodifiableConfig section) {
      port(section, node);
    } else if (value instanceof List<?> list) {
      node.raw(List.of());
      for (final Object element : list) {
        portValue(element, node.appendListNode());
      }
    } else if (value instanceof String string) {
      node.set(string);
      if (node.parent() != null && node.parent().isMap()) {
        node.hint(YamlConfigurationLoader.SCALAR_STYLE, ScalarStyle.DOUBLE_QUOTED);
      }
    } else if (value != null) {
      node.set(value);
    }
  }

  private static TransformAction moveTo(final Object... newPath) {
    return (oldPath, value) -> newPath;
  }

  /**
   * A TOML key outside {@code [A-Za-z0-9_-]} must be quoted, and the TOML parser returns it with
   * its quotes.
   */
  private static String cleanKey(final String key) {
    return key.replace("\"", "");
  }

  /**
   * The TOML reader fell back to values of its own for some options missing from the file, and
   * these differ from {@code default-velocity.yaml}. Writing them out keeps such a proxy
   * behaving exactly as it did.
   */
  private static void keepLegacyFallbacks(final UnmodifiableConfig legacy,
      final ConfigurationNode converted) throws SerializationException {
    setIfAbsent(converted.node("announce-forge"), true);
    if (legacy.contains("advanced")) {
      setIfAbsent(converted.node("advanced", "command-rate-limit"), 25);
    } else {
      // Without an [advanced] table at all, the reader used the field initializers instead.
      setIfAbsent(converted.node("advanced", "kick-after-rate-limited-commands"), 5);
      setIfAbsent(converted.node("advanced", "tab-complete-rate-limit"), 50);
      setIfAbsent(converted.node("advanced", "kick-after-rate-limited-tab-completes"), 10);
    }
  }

  private static void setIfAbsent(final ConfigurationNode node, final Object value)
      throws SerializationException {
    if (node.virtual()) {
      node.set(value);
    }
  }

  /**
   * The TOML reader accepted {@code advanced.proxy-protocol} when
   * {@code advanced.haproxy-protocol} was missing.
   */
  private static void renameProxyProtocol(final ConfigurationNode converted)
      throws SerializationException {
    final ConfigurationNode legacy = converted.node("advanced", "proxy-protocol");
    if (legacy.virtual()) {
      return;
    }
    final ConfigurationNode current = converted.node("advanced", "haproxy-protocol");
    if (current.virtual()) {
      current.set(legacy.raw());
    }
    converted.node("advanced").removeChild("proxy-protocol");
  }

  /**
   * Puts the options of {@code target} in the order the shipped defaults list them, followed by
   * any the defaults do not know. Options moved or filled in during the conversion would
   * otherwise end up at the bottom of their section.
   */
  private static void orderLike(final ConfigurationNode defaults, final ConfigurationNode target)
      throws SerializationException {
    final Map<Object, ConfigurationNode> ordered = new LinkedHashMap<>();
    for (final Object key : defaults.childrenMap().keySet()) {
      if (!target.node(key).virtual()) {
        ordered.put(key, target.node(key).copy());
      }
    }
    for (final Map.Entry<Object, ? extends ConfigurationNode> entry
        : target.childrenMap().entrySet()) {
      ordered.putIfAbsent(entry.getKey(), entry.getValue().copy());
    }
    for (final Object key : ordered.keySet()) {
      target.removeChild(key);
    }
    for (final Map.Entry<Object, ConfigurationNode> entry : ordered.entrySet()) {
      final ConfigurationNode child = target.node(entry.getKey()).from(entry.getValue());
      final ConfigurationNode defaultChild = defaults.node(entry.getKey());
      if (defaultChild.isMap() && child.isMap()
          && !VelocityConfiguration.USER_NAMED_SECTIONS.contains(entry.getKey())) {
        orderLike(defaultChild, child);
      }
    }
  }

  /**
   * Gives every option of {@code target} that the shipped defaults document the same comment.
   */
  private static void copyComments(final CommentedConfigurationNode defaults,
      final CommentedConfigurationNode target) {
    for (final Map.Entry<Object, CommentedConfigurationNode> entry
        : defaults.childrenMap().entrySet()) {
      final CommentedConfigurationNode child = target.node(entry.getKey());
      if (child.virtual()) {
        continue;
      }
      final @Nullable String comment = entry.getValue().comment();
      if (comment != null) {
        child.commentIfAbsent(comment);
      }
      if (entry.getValue().isMap() && child.isMap()) {
        copyComments(entry.getValue(), child);
      }
    }
  }

  /**
   * Keeps the TOML as {@code velocity.toml.bak}. It is copied rather than moved, and a failure
   * to remove it only warns: a file mounted into a container cannot be moved or deleted.
   */
  private static void backUp(final Path legacyPath, final Logger logger) {
    final Path backupPath = legacyPath.resolveSibling(legacyPath.getFileName() + BACKUP_SUFFIX);
    try {
      Files.copy(legacyPath, backupPath, StandardCopyOption.REPLACE_EXISTING);
    } catch (final IOException e) {
      logger.warn("Could not back up {} to {}; it is left in place and no longer read.",
          legacyPath.getFileName(), backupPath.getFileName(), e);
      return;
    }
    try {
      Files.delete(legacyPath);
      logger.info("The old configuration has been kept as {}.", backupPath.getFileName());
    } catch (final IOException e) {
      logger.warn("The old configuration has been kept as {}, but {} could not be removed. It "
          + "is no longer read; remove it yourself.", backupPath.getFileName(),
          legacyPath.getFileName(), e);
    }
  }
}
