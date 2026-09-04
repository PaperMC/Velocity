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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.serialize.TypeSerializer;
import org.spongepowered.configurate.yaml.NodeStyle;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

/**
 * A YAML document addressed by dotted paths, preserving comments and blank lines across a
 * load/save cycle. A key containing a dot is only reachable through its parent section.
 */
public final class YamlDocument implements ConfigurationDocument {

  private static final String PATH_SEPARATOR = ".";
  private static final Pattern PATH_SPLITTER = Pattern.compile(Pattern.quote(PATH_SEPARATOR));
  private static final int INDENT = 2;
  private static final int MAXIMUM_LINE_WIDTH = 4096;

  private final CommentedConfigurationNode root;

  private YamlDocument(final CommentedConfigurationNode root) {
    this.root = root;
  }

  /**
   * Reads a document from {@code path}.
   */
  public static YamlDocument load(final Path path) throws IOException {
    try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
      return read(reader);
    }
  }

  /**
   * Reads a document from {@code reader}.
   */
  public static YamlDocument read(final Reader reader) throws IOException {
    final BufferedReader buffered =
        reader instanceof BufferedReader ? (BufferedReader) reader : new BufferedReader(reader);
    final CommentedConfigurationNode root = loader().source(() -> buffered).build().load();
    discardBlankComments(root);
    return new YamlDocument(root);
  }

  /**
   * Builds a document from {@code values}, nesting a {@link Map} value as a section. Unlike
   * {@link #set(String, Object)} the keys are taken as-is, dots included.
   */
  public static YamlDocument of(final Map<String, ?> values) {
    final CommentedConfigurationNode root = loader().build().createNode();
    root.raw(values);
    return new YamlDocument(root);
  }

  @Override
  public void save(final Path path) throws IOException {
    try (BufferedWriter writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
      write(writer);
    }
  }

  @Override
  public void write(final Writer writer) throws IOException {
    loader().sink(() -> new BufferedWriter(writer)).build().save(root);
  }

  @Override
  public boolean contains(final String path) {
    return resolve(path) != null;
  }

  @Override
  public String getString(final String path) {
    return require(path, node -> node.require(String.class), "a single value");
  }

  @Override
  public int getInt(final String path) {
    return require(path, node -> node.require(Integer.class), "a whole number");
  }

  @Override
  public boolean getBoolean(final String path) {
    return require(path, node -> node.require(Boolean.class), "true or false");
  }

  @Override
  public List<String> getStringList(final String path) {
    return require(path, node -> node.getList(String.class), "a list of single values");
  }

  @Override
  public <T extends Enum<T>> T getEnum(final String path, final Class<T> type) {
    return require(path, node -> node.require(type), "one of " + constantsOf(type));
  }

  @Override
  public YamlDocument getSection(final String path) {
    final CommentedConfigurationNode node = resolve(path);
    if (node == null) {
      throw new IllegalStateException("No configuration option '" + path + "'.");
    }
    if (!node.isMap()) {
      throw new IllegalArgumentException("Configuration option '" + path
          + "' must be a section, but is '" + node.raw() + "'.");
    }
    return new YamlDocument(node);
  }

  @Override
  public Set<String> keys() {
    final Set<String> keys = new LinkedHashSet<>();
    for (final Object key : root.childrenMap().keySet()) {
      keys.add(String.valueOf(key));
    }
    return keys;
  }

  /**
   * Reads {@code path} through a Configurate {@link TypeSerializer}, naming {@code expectation}
   * as the type when it does not fit.
   */
  private <T> T require(final String path, final NodeReader<T> reader, final String expectation) {
    final CommentedConfigurationNode node = resolve(path);
    if (node == null) {
      throw new IllegalStateException("No configuration option '" + path + "'.");
    }
    final T value;
    try {
      value = reader.read(node);
    } catch (final SerializationException | NoSuchElementException e) {
      throw new IllegalArgumentException("Configuration option '" + path + "' must be "
          + expectation + ", but is '" + node.raw() + "'.", e);
    }
    if (value == null) {
      throw new IllegalStateException("No configuration option '" + path + "'.");
    }
    return value;
  }

  private static String constantsOf(final Class<? extends Enum<?>> type) {
    return Arrays.stream(type.getEnumConstants())
        .map(constant -> constant.name().toLowerCase(Locale.ROOT))
        .collect(Collectors.joining(", "));
  }

  @Override
  public void set(final String path, final @Nullable Object value) {
    node(path).raw(value);
  }

  @Override
  public void remove(final String path) {
    node(path).raw(null);
  }

  @Override
  public @Nullable String getComment(final String path) {
    final CommentedConfigurationNode node = resolve(path);
    return node == null ? null : node.comment();
  }

  @Override
  public void setComment(final String path, final @Nullable String comment) {
    final CommentedConfigurationNode node = resolve(path);
    if (node != null) {
      node.comment(comment);
    }
  }

  @Override
  public Set<String> paths() {
    return collectPaths(false);
  }

  @Override
  public Set<String> leafPaths() {
    return collectPaths(true);
  }

  private Set<String> collectPaths(final boolean leavesOnly) {
    final Set<String> paths = new LinkedHashSet<>();
    collectPaths(root, "", leavesOnly, paths);
    return paths;
  }

  private static void collectPaths(final CommentedConfigurationNode parent, final String prefix,
      final boolean leavesOnly, final Set<String> paths) {
    for (final Map.Entry<Object, CommentedConfigurationNode> child
        : parent.childrenMap().entrySet()) {
      final String path = prefix + child.getKey();
      final boolean section = child.getValue().isMap();
      if (!section || !leavesOnly) {
        paths.add(path);
      }
      if (section) {
        collectPaths(child.getValue(), path + PATH_SEPARATOR, leavesOnly, paths);
      }
    }
  }

  private @Nullable CommentedConfigurationNode resolve(final String path) {
    final CommentedConfigurationNode node = node(path);
    return node.virtual() ? null : node;
  }

  private CommentedConfigurationNode node(final String path) {
    final CommentedConfigurationNode literal = root.node(path);
    return literal.virtual() ? root.node((Object[]) PATH_SPLITTER.split(path)) : literal;
  }

  /**
   * A blank line above an uncommented key reads back as an empty comment, and the emitter
   * writes a blank line above every commented key of its own accord, so keeping it would add a
   * second blank line on each save.
   */
  private static void discardBlankComments(final CommentedConfigurationNode node) {
    final String comment = node.comment();
    if (comment != null && comment.isBlank()) {
      node.comment(null);
    }
    for (final CommentedConfigurationNode child : node.childrenMap().values()) {
      discardBlankComments(child);
    }
  }

  private static YamlConfigurationLoader.Builder loader() {
    return YamlConfigurationLoader.builder()
        .indent(INDENT)
        .nodeStyle(NodeStyle.BLOCK)
        .commentsEnabled(true)
        .lineLength(MAXIMUM_LINE_WIDTH);
  }

  /**
   * Deserializes one node, deferring to Configurate for the conversion the type asks for.
   */
  @FunctionalInterface
  private interface NodeReader<T> {

    @Nullable T read(CommentedConfigurationNode node) throws SerializationException;
  }
}
