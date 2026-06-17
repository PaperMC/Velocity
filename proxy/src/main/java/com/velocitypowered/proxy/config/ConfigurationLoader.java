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
import java.lang.reflect.Type;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
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
      List<String> attemptConnectionOrder = ImmutableList.of("lobby");
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
