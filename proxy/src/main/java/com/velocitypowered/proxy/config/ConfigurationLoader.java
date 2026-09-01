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

package com.velocitypowered.proxy.config;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.objectmapping.ObjectMapper;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.serialize.TypeSerializer;
import org.spongepowered.configurate.util.NamingSchemes;
import org.spongepowered.configurate.yaml.NodeStyle;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

/**
 * Velocity Configurate (YAML) loader entry utils.
 */
public final class ConfigurationLoader {

  private static final Logger logger = LogManager.getLogger(ConfigurationLoader.class);

  private static final String DEFAULT_CONFIG_RESOURCE = "default-velocity.yaml";
  private static final Path DEFAULT_CONFIG_PATH = Path.of("velocity.yaml");
  private static final Path LEGACY_CONFIG_PATH = Path.of("velocity.toml");
  private static final String FORWARDING_SECRET_FILE_KEY = "forwarding-secret-file";
  private static final String DEFAULT_FORWARDING_SECRET_FILE = "forwarding.secret";
  static final String CONFIG_VERSION_KEY = "config-version";

  /**
   * Current {@code velocity.yaml} schema version. Files are born at this version (either freshly
   * written from the bundled default or stamped during a {@code velocity.toml} migration). Once a
   * YAML-schema migration is needed, drive upgrades from here via
   * {@link org.spongepowered.configurate.transformation.ConfigurationTransformation#versionedBuilder()}
   * keyed on {@link #CONFIG_VERSION_KEY}.
   */
  static final int CURRENT_CONFIG_VERSION = 1;

  /**
   * ObjectMapper factory configured to map {@code camelCase} fields onto {@code lower-case-dashed}
   * configuration keys, matching the historical TOML key style.
   */
  private static final ObjectMapper.Factory OBJECT_MAPPER_FACTORY = ObjectMapper.factoryBuilder()
      .defaultNamingScheme(NamingSchemes.LOWER_CASE_DASHED)
      .build();

  private ConfigurationLoader() {
  }

  /**
   * Loads the Velocity configuration from {@code velocity.yaml}, migrating a legacy
   * {@code velocity.toml} or writing the documented default on first start as needed.
   *
   * @return the loaded configuration
   * @throws IOException if the configuration could not be read or written
   */
  public static VelocityConfiguration loadConfiguration() throws IOException {
    return loadConfiguration(DEFAULT_CONFIG_PATH, LEGACY_CONFIG_PATH);
  }

  /**
   * Loads the Velocity configuration from {@code path}, migrating {@code legacyPath} or writing the
   * documented default if {@code path} does not yet exist.
   *
   * @param path the YAML configuration path
   * @param legacyPath the legacy TOML configuration path to migrate from, if present
   * @return the loaded configuration
   * @throws IOException if the configuration could not be read or written
   */
  static VelocityConfiguration loadConfiguration(final Path path, final Path legacyPath)
      throws IOException {
    if (Files.notExists(path) && Files.exists(legacyPath)) {
      migrateFromLegacy(legacyPath, path);
    }

    if (Files.notExists(path)) {
      // Fresh install: copy the documented default verbatim so its comments are preserved.
      writeDefaultConfig(path);
    }

    // Absent keys fall back to the model's field defaults (matching the old getOrElse behaviour),
    // so there is no need to merge the bundled default back into the user's file here.
    final CommentedConfigurationNode node = yamlLoader(path).build().load();
    final VelocityConfiguration config = node.get(VelocityConfiguration.class);
    if (config == null) {
      throw new IOException("Unable to deserialize configuration from " + path);
    }
    config.setForwardingSecret(readForwardingSecret(node));
    return config;
  }

  /**
   * Converts a legacy {@code velocity.toml} to {@code velocity.yaml}. The legacy night-config
   * migrations are run first to normalise the file, then it is written out as YAML stamped with the
   * current schema version and the old file is preserved as {@code velocity.toml.migrated}.
   */
  private static void migrateFromLegacy(final Path legacyPath, final Path path) throws IOException {
    logger.info("Found a legacy {}; migrating it to {}.",
        legacyPath.getFileName(), path.getFileName());
    final VelocityConfiguration legacy = LegacyConfigurationLoader.read(legacyPath);

    final YamlConfigurationLoader loader = yamlLoader(path).build();
    final CommentedConfigurationNode node = loader.createNode();
    node.set(VelocityConfiguration.class, legacy);
    // forwarding-secret-file is a bootstrap pointer, not a mapped field; carry it across so a
    // custom secret location keeps working after migration.
    final String secretFile = LegacyConfigurationLoader.readForwardingSecretFile(legacyPath);
    if (secretFile != null) {
      node.node(FORWARDING_SECRET_FILE_KEY).set(secretFile);
    }
    node.node(CONFIG_VERSION_KEY).set(CURRENT_CONFIG_VERSION);
    loader.save(node);

    final Path archived = legacyPath.resolveSibling(legacyPath.getFileName().toString()
        + ".migrated");
    Files.move(legacyPath, archived, StandardCopyOption.REPLACE_EXISTING);
    logger.info("Migration complete. Your old configuration has been preserved as {}.",
        archived.getFileName());
  }

  private static void writeDefaultConfig(final Path path) throws IOException {
    try (InputStream in = ConfigurationLoader.class.getClassLoader()
        .getResourceAsStream(DEFAULT_CONFIG_RESOURCE)) {
      if (in == null) {
        throw new IOException(DEFAULT_CONFIG_RESOURCE + " is missing from the classpath");
      }
      Files.copy(in, path);
    }
  }

  /**
   * Resolves the player-info forwarding secret. Mirrors the legacy behaviour: prefer the
   * {@code VELOCITY_FORWARDING_SECRET} environment variable, otherwise read (creating if absent) the
   * file pointed at by {@code forwarding-secret-file}. The secret is deliberately kept out of
   * {@code velocity.yaml}.
   */
  private static byte[] readForwardingSecret(final ConfigurationNode node) throws IOException {
    String secret = System.getenv().getOrDefault("VELOCITY_FORWARDING_SECRET", "");
    if (secret.isBlank()) {
      final String secretFile = node.node(FORWARDING_SECRET_FILE_KEY)
          .getString(DEFAULT_FORWARDING_SECRET_FILE);
      final Path secretPath = Path.of(secretFile);
      if (Files.exists(secretPath)) {
        if (Files.isRegularFile(secretPath)) {
          secret = String.join("", Files.readAllLines(secretPath));
        } else {
          throw new IOException(
              "The file " + secretFile + " is not a valid file or it is a directory.");
        }
      } else {
        Files.createFile(secretPath);
        Files.writeString(secretPath, secret = VelocityConfiguration.generateRandomString(12),
            StandardCharsets.UTF_8);
        logger.info("The forwarding-secret-file does not exist. A new file has been created at {}",
            secretFile);
      }
    }
    return secret.getBytes(StandardCharsets.UTF_8);
  }

  /**
   * Builds a YAML loader for the given {@code path}, wired with the serializers needed to map a
   * {@link VelocityConfiguration}.
   *
   * @param path the configuration file path
   * @return the configured loader builder
   */
  static YamlConfigurationLoader.Builder yamlLoader(final Path path) {
    return YamlConfigurationLoader.builder()
        .path(path)
        .nodeStyle(NodeStyle.BLOCK)
        .indent(2)
        .defaultOptions(opts -> opts.serializers(builder -> builder
            .register(VelocityConfiguration.Servers.class, new ServersSerializer())
            .register(VelocityConfiguration.ForcedHosts.class, new ForcedHostsSerializer())
            .register(VelocityConfiguration.PacketLimiterConfig.class,
                new PacketLimiterConfigSerializer())
            .registerAnnotatedObjects(OBJECT_MAPPER_FACTORY)));
  }

  /**
   * Reads a {@link VelocityConfiguration} from the YAML file at {@code path}.
   *
   * @param path the configuration file path
   * @return the deserialized configuration
   * @throws IOException if the file could not be read or deserialized
   */
  static VelocityConfiguration load(final Path path) throws IOException {
    final CommentedConfigurationNode node = yamlLoader(path).build().load();
    final VelocityConfiguration config = node.get(VelocityConfiguration.class);
    if (config == null) {
      throw new IOException("Unable to deserialize configuration from " + path);
    }
    return config;
  }

  /**
   * Writes a {@link VelocityConfiguration} to the YAML file at {@code path}.
   *
   * @param config the configuration to write
   * @param path the configuration file path
   * @throws IOException if the file could not be written
   */
  static void save(final VelocityConfiguration config, final Path path) throws IOException {
    final YamlConfigurationLoader loader = yamlLoader(path).build();
    final CommentedConfigurationNode node = loader.createNode();
    node.set(VelocityConfiguration.class, config);
    loader.save(node);
  }

  /**
   * Serializes {@code config} onto the provided {@code node}. Exposed for migration tooling.
   *
   * @param config the configuration to serialize
   * @param node the node to write to
   * @throws SerializationException if serialization fails
   */
  static void write(final VelocityConfiguration config, final ConfigurationNode node)
      throws SerializationException {
    node.set(VelocityConfiguration.class, config);
  }

  /**
   * Serializes the dynamic {@code [servers]} section, where named server entries live alongside the
   * {@code try} fallback order in a single node.
   */
  static final class ServersSerializer implements TypeSerializer<VelocityConfiguration.Servers> {

    @Override
    public VelocityConfiguration.Servers deserialize(final Type type, final ConfigurationNode node)
        throws SerializationException {
      final Map<String, String> servers = new LinkedHashMap<>();
      List<String> attemptConnectionOrder = ImmutableList.of();
      for (final Map.Entry<Object, ? extends ConfigurationNode> entry
          : node.childrenMap().entrySet()) {
        final String key = entry.getKey().toString();
        final ConfigurationNode value = entry.getValue();
        if (key.equalsIgnoreCase("try")) {
          attemptConnectionOrder = value.getList(String.class, ImmutableList.of());
        } else {
          final String address = value.getString();
          if (address == null) {
            throw new SerializationException("Server entry " + key + " is not a string!");
          }
          servers.put(VelocityConfiguration.Servers.cleanServerName(key), address);
        }
      }
      return new VelocityConfiguration.Servers(ImmutableMap.copyOf(servers),
          ImmutableList.copyOf(attemptConnectionOrder));
    }

    @Override
    public void serialize(final Type type, final VelocityConfiguration.@Nullable Servers obj,
        final ConfigurationNode node) throws SerializationException {
      if (obj == null) {
        node.raw(null);
        return;
      }
      for (final Map.Entry<String, String> entry : obj.getServers().entrySet()) {
        node.node(entry.getKey()).set(entry.getValue());
      }
      node.node("try").setList(String.class, obj.getAttemptConnectionOrder());
    }
  }

  /**
   * Serializes the dynamic {@code [forced-hosts]} section (host pattern to server list map).
   */
  static final class ForcedHostsSerializer
      implements TypeSerializer<VelocityConfiguration.ForcedHosts> {

    @Override
    public VelocityConfiguration.ForcedHosts deserialize(final Type type,
        final ConfigurationNode node) throws SerializationException {
      final Map<String, List<String>> forcedHosts = new LinkedHashMap<>();
      for (final Map.Entry<Object, ? extends ConfigurationNode> entry
          : node.childrenMap().entrySet()) {
        final String key = entry.getKey().toString().toLowerCase(Locale.ROOT);
        forcedHosts.put(key,
            ImmutableList.copyOf(entry.getValue().getList(String.class, ImmutableList.of())));
      }
      return new VelocityConfiguration.ForcedHosts(ImmutableMap.copyOf(forcedHosts));
    }

    @Override
    public void serialize(final Type type, final VelocityConfiguration.@Nullable ForcedHosts obj,
        final ConfigurationNode node) throws SerializationException {
      if (obj == null) {
        node.raw(null);
        return;
      }
      for (final Map.Entry<String, List<String>> entry : obj.getForcedHosts().entrySet()) {
        node.node(entry.getKey()).setList(String.class, entry.getValue());
      }
    }
  }

  /**
   * Serializes the {@code [packet-limiter]} section, whose keys do not follow the field naming
   * scheme (for example {@code packets-per-second} maps to {@code pps}).
   */
  static final class PacketLimiterConfigSerializer
      implements TypeSerializer<VelocityConfiguration.PacketLimiterConfig> {

    @Override
    public VelocityConfiguration.PacketLimiterConfig deserialize(final Type type,
        final ConfigurationNode node) {
      final VelocityConfiguration.PacketLimiterConfig def =
          VelocityConfiguration.PacketLimiterConfig.DEFAULT;
      return new VelocityConfiguration.PacketLimiterConfig(
          node.node("interval").getInt(def.interval()),
          node.node("packets-per-second").getInt(def.pps()),
          node.node("bytes-per-second").getInt(def.bytes()),
          node.node("decompressed-bytes-per-second").getInt(def.bytesAfterDecompression()));
    }

    @Override
    public void serialize(final Type type,
        final VelocityConfiguration.@Nullable PacketLimiterConfig obj, final ConfigurationNode node)
        throws SerializationException {
      if (obj == null) {
        node.raw(null);
        return;
      }
      node.node("interval").set(obj.interval());
      node.node("packets-per-second").set(obj.pps());
      node.node("bytes-per-second").set(obj.bytes());
      node.node("decompressed-bytes-per-second").set(obj.bytesAfterDecompression());
    }
  }
}
