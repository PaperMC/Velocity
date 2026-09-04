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
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.comments.CommentLine;
import org.yaml.snakeyaml.comments.CommentType;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.nodes.MappingNode;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.NodeTuple;
import org.yaml.snakeyaml.nodes.ScalarNode;
import org.yaml.snakeyaml.nodes.SequenceNode;
import org.yaml.snakeyaml.nodes.Tag;
import org.yaml.snakeyaml.representer.Representer;

/**
 * A YAML document addressed by dotted paths, preserving comments and blank lines across a
 * load/save cycle. A key containing a dot is only reachable through its parent section.
 */
public final class YamlDocument implements Configuration {

  private static final String PATH_SEPARATOR = ".";
  private static final Pattern PATH_SPLITTER = Pattern.compile(Pattern.quote(PATH_SEPARATOR));
  private static final Pattern TRAILING_WHITESPACE = Pattern.compile("[ \\t]+$", Pattern.MULTILINE);
  private static final int MAXIMUM_LINE_WIDTH = 4096;

  private final MappingNode root;

  private YamlDocument(final MappingNode root) {
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
  public static YamlDocument read(final Reader reader) {
    final Node node = createYaml().compose(reader);
    if (node == null) {
      return new YamlDocument(emptyMapping());
    }
    if (!(node instanceof MappingNode mapping)) {
      throw new IllegalArgumentException("The root of a configuration must be a mapping.");
    }
    return new YamlDocument(mapping);
  }

  /**
   * Builds a document from {@code values}, nesting a {@link Map} value as a section. Unlike
   * {@link #set(String, Object)} the keys are taken as-is, dots included.
   */
  public static YamlDocument of(final Map<String, ?> values) {
    final YamlDocument document = new YamlDocument(emptyMapping());
    for (final Map.Entry<String, ?> entry : values.entrySet()) {
      put(document.root, entry.getKey(), fromJava(entry.getValue()));
    }
    return document;
  }

  /**
   * Writes this document to {@code path}.
   */
  public void save(final Path path) throws IOException {
    try (BufferedWriter writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
      write(writer);
    }
  }

  /**
   * Writes this document to {@code writer}.
   */
  public void write(final Writer writer) throws IOException {
    final StringWriter buffer = new StringWriter();
    createYaml().serialize(root, buffer);
    writer.write(TRAILING_WHITESPACE.matcher(buffer.toString()).replaceAll(""));
  }

  public @Nullable Object get(final String path) {
    final Node node = resolve(path);
    return node == null ? null : toJava(node);
  }

  @Override
  public boolean contains(final String path) {
    return resolve(path) != null;
  }

  @Override
  public String getString(final String path) {
    final Object value = require(path);
    if (value instanceof List || value instanceof Map) {
      throw new IllegalArgumentException("Configuration option '" + path
          + "' must be a single value, but is '" + value + "'.");
    }
    return String.valueOf(value);
  }

  @Override
  public int getInt(final String path) {
    final Object value = require(path);
    if (value instanceof Number number) {
      return number.intValue();
    }
    throw new IllegalArgumentException(
        "Configuration option '" + path + "' must be a number, but is '" + value + "'.");
  }

  @Override
  public boolean getBoolean(final String path) {
    final Object value = require(path);
    if (value instanceof Boolean bool) {
      return bool;
    }
    throw new IllegalArgumentException(
        "Configuration option '" + path + "' must be true or false, but is '" + value + "'.");
  }

  @Override
  public List<String> getStringList(final String path) {
    final Object value = require(path);
    if (!(value instanceof List<?> list)) {
      return List.of(getString(path));
    }
    final List<String> values = new ArrayList<>(list.size());
    for (final Object element : list) {
      values.add(String.valueOf(element));
    }
    return values;
  }

  @Override
  public <T extends Enum<T>> T getEnum(final String path, final Class<T> type) {
    final String value = getString(path);
    try {
      return Enum.valueOf(type, value.toUpperCase(Locale.ROOT));
    } catch (final IllegalArgumentException e) {
      throw new IllegalArgumentException("Configuration option '" + path + "' has unknown value '"
          + value + "'.", e);
    }
  }

  @Override
  public ConfigurationSection getSection(final String path) {
    final Node node = resolve(path);
    if (node == null) {
      throw new IllegalStateException("No configuration option '" + path + "'.");
    }
    if (!(node instanceof MappingNode mapping)) {
      throw new IllegalArgumentException("Configuration option '" + path
          + "' must be a section, but is '" + toJava(node) + "'.");
    }
    return new YamlDocument(mapping);
  }

  @Override
  public Set<String> keys() {
    final Set<String> keys = new LinkedHashSet<>();
    for (final NodeTuple tuple : root.getValue()) {
      keys.add(keyOf(tuple));
    }
    return keys;
  }

  private Object require(final String path) {
    final Object value = get(path);
    if (value == null) {
      throw new IllegalStateException("No configuration option '" + path + "'.");
    }
    return value;
  }

  /**
   * Sets the value at {@code path}, creating missing parent sections. An existing key keeps its
   * comments and, where the type is unchanged, its quoting.
   */
  public void set(final String path, final @Nullable Object value) {
    final String[] keys = PATH_SPLITTER.split(path);
    MappingNode parent = root;
    for (int i = 0; i < keys.length - 1; i++) {
      final NodeTuple tuple = find(parent, keys[i]);
      if (tuple != null && tuple.getValueNode() instanceof MappingNode section) {
        parent = section;
      } else {
        final MappingNode section = emptyMapping();
        put(parent, keys[i], section);
        parent = section;
      }
    }
    put(parent, keys[keys.length - 1], fromJava(value));
  }

  /**
   * Removes the value at {@code path}, along with its comments.
   */
  public void remove(final String path) {
    final int lastSeparator = path.lastIndexOf(PATH_SEPARATOR);
    final Node parent = lastSeparator == -1 ? root : resolve(path.substring(0, lastSeparator));
    if (!(parent instanceof MappingNode mapping)) {
      return;
    }
    final String key = path.substring(lastSeparator + 1);
    final List<NodeTuple> remaining = new ArrayList<>(mapping.getValue());
    remaining.removeIf(tuple -> key.equals(keyOf(tuple)));
    mapping.setValue(remaining);
  }

  /**
   * Returns the comment at {@code path} as one element per line, an empty element being a blank
   * line, or {@code null} if the key is absent or uncommented.
   */
  public @Nullable List<String> getComment(final String path) {
    final NodeTuple tuple = resolveTuple(path);
    if (tuple == null) {
      return null;
    }
    final List<CommentLine> comments = tuple.getKeyNode().getBlockComments();
    if (comments == null || comments.isEmpty()) {
      return null;
    }
    final List<String> lines = new ArrayList<>(comments.size());
    for (final CommentLine comment : comments) {
      lines.add(comment.getCommentType() == CommentType.BLANK_LINE ? "" : comment.getValue());
    }
    return lines;
  }

  /**
   * Attaches a comment to {@code path}, replacing any existing one.
   */
  public void setComment(final String path, final List<String> lines) {
    final NodeTuple tuple = resolveTuple(path);
    if (tuple == null) {
      return;
    }
    final List<CommentLine> comments = new ArrayList<>(lines.size());
    for (final String line : lines) {
      comments.add(line.isEmpty()
          ? new CommentLine(null, null, "\n", CommentType.BLANK_LINE)
          : new CommentLine(null, null, line, CommentType.BLOCK));
    }
    tuple.getKeyNode().setBlockComments(comments);
  }

  /**
   * Copies the comments of {@code source} onto the keys both documents share, matching keys
   * structurally so a key containing a dot is handled like any other. Anything else is left alone.
   */
  public void copyCommentsFrom(final YamlDocument source) {
    copyComments(source.root, root);
  }

  private static void copyComments(final MappingNode source, final MappingNode target) {
    for (final NodeTuple sourceTuple : source.getValue()) {
      final NodeTuple targetTuple = find(target, keyOf(sourceTuple));
      if (targetTuple == null) {
        continue;
      }
      final List<CommentLine> comments = sourceTuple.getKeyNode().getBlockComments();
      if (comments != null && !comments.isEmpty()) {
        targetTuple.getKeyNode().setBlockComments(new ArrayList<>(comments));
      }
      if (sourceTuple.getValueNode() instanceof MappingNode sourceSection
          && targetTuple.getValueNode() instanceof MappingNode targetSection) {
        copyComments(sourceSection, targetSection);
      }
    }
  }

  /**
   * Returns, in document order, the dotted path of every value that is not itself a section.
   */
  public Set<String> leafPaths() {
    final Set<String> paths = new LinkedHashSet<>();
    collectLeafPaths(root, "", paths);
    return paths;
  }

  private static void collectLeafPaths(final MappingNode mapping, final String prefix,
      final Set<String> paths) {
    for (final NodeTuple tuple : mapping.getValue()) {
      final String path = prefix + keyOf(tuple);
      if (tuple.getValueNode() instanceof MappingNode section) {
        collectLeafPaths(section, path + PATH_SEPARATOR, paths);
      } else {
        paths.add(path);
      }
    }
  }

  private @Nullable Node resolve(final String path) {
    final NodeTuple tuple = resolveTuple(path);
    return tuple == null ? null : tuple.getValueNode();
  }

  private @Nullable NodeTuple resolveTuple(final String path) {
    final NodeTuple literal = find(root, path);
    if (literal != null) {
      return literal;
    }
    Node current = root;
    NodeTuple tuple = null;
    for (final String key : PATH_SPLITTER.split(path)) {
      if (!(current instanceof MappingNode mapping)) {
        return null;
      }
      tuple = find(mapping, key);
      if (tuple == null) {
        return null;
      }
      current = tuple.getValueNode();
    }
    return tuple;
  }

  private static @Nullable NodeTuple find(final MappingNode mapping, final String key) {
    for (final NodeTuple tuple : mapping.getValue()) {
      if (key.equals(keyOf(tuple))) {
        return tuple;
      }
    }
    return null;
  }

  private static void put(final MappingNode mapping, final String key, final Node value) {
    final List<NodeTuple> tuples = new ArrayList<>(mapping.getValue());
    for (int i = 0; i < tuples.size(); i++) {
      final NodeTuple tuple = tuples.get(i);
      if (key.equals(keyOf(tuple))) {
        tuples.set(i, new NodeTuple(tuple.getKeyNode(), styleAs(tuple.getValueNode(), value)));
        mapping.setValue(tuples);
        return;
      }
    }
    tuples.add(new NodeTuple(scalar(Tag.STR, key), value));
    mapping.setValue(tuples);
  }

  private static Node styleAs(final Node previous, final Node replacement) {
    if (previous instanceof ScalarNode old && replacement instanceof ScalarNode current
        && old.getTag().equals(current.getTag())) {
      return new ScalarNode(current.getTag(), current.getValue(), null, null, old.getScalarStyle());
    }
    return replacement;
  }

  private static String keyOf(final NodeTuple tuple) {
    return tuple.getKeyNode() instanceof ScalarNode scalar ? scalar.getValue() : "";
  }

  private static @Nullable Object toJava(final Node node) {
    if (node instanceof ScalarNode scalar) {
      return scalarToJava(scalar);
    }
    if (node instanceof SequenceNode sequence) {
      final List<Object> values = new ArrayList<>(sequence.getValue().size());
      for (final Node element : sequence.getValue()) {
        values.add(toJava(element));
      }
      return values;
    }
    final Map<String, Object> values = new LinkedHashMap<>();
    for (final NodeTuple tuple : ((MappingNode) node).getValue()) {
      values.put(keyOf(tuple), toJava(tuple.getValueNode()));
    }
    return values;
  }

  private static @Nullable Object scalarToJava(final ScalarNode scalar) {
    final String value = scalar.getValue();
    final Tag tag = scalar.getTag();
    if (Tag.NULL.equals(tag)) {
      return null;
    }
    if (Tag.BOOL.equals(tag)) {
      final String normalized = value.toLowerCase(Locale.ROOT);
      return normalized.equals("true") || normalized.equals("yes") || normalized.equals("on");
    }
    if (Tag.INT.equals(tag)) {
      final long parsed = Long.parseLong(value.replace("_", ""));
      if (parsed >= Integer.MIN_VALUE && parsed <= Integer.MAX_VALUE) {
        return (int) parsed;
      }
      return parsed;
    }
    if (Tag.FLOAT.equals(tag)) {
      return Double.parseDouble(value.replace("_", ""));
    }
    return value;
  }

  private static Node fromJava(final @Nullable Object value) {
    if (value == null) {
      return scalar(Tag.NULL, "null");
    }
    if (value instanceof Boolean bool) {
      return scalar(Tag.BOOL, bool.toString());
    }
    if (value instanceof Integer || value instanceof Long || value instanceof Short
        || value instanceof Byte) {
      return scalar(Tag.INT, value.toString());
    }
    if (value instanceof Float || value instanceof Double) {
      return scalar(Tag.FLOAT, value.toString());
    }
    if (value instanceof List<?> list) {
      final List<Node> elements = new ArrayList<>(list.size());
      for (final Object element : list) {
        elements.add(fromJava(element));
      }
      return new SequenceNode(Tag.SEQ, elements, DumperOptions.FlowStyle.BLOCK);
    }
    if (value instanceof Map<?, ?> map) {
      final List<NodeTuple> tuples = new ArrayList<>(map.size());
      for (final Map.Entry<?, ?> entry : map.entrySet()) {
        tuples.add(new NodeTuple(scalar(Tag.STR, entry.getKey().toString()),
            fromJava(entry.getValue())));
      }
      return new MappingNode(Tag.MAP, tuples, DumperOptions.FlowStyle.BLOCK);
    }
    return scalar(Tag.STR, value.toString());
  }

  private static ScalarNode scalar(final Tag tag, final String value) {
    return new ScalarNode(tag, value, null, null, DumperOptions.ScalarStyle.PLAIN);
  }

  private static MappingNode emptyMapping() {
    return new MappingNode(Tag.MAP, new ArrayList<>(), DumperOptions.FlowStyle.BLOCK);
  }

  private static Yaml createYaml() {
    final LoaderOptions loaderOptions = new LoaderOptions();
    loaderOptions.setProcessComments(true);

    final DumperOptions dumperOptions = new DumperOptions();
    dumperOptions.setProcessComments(true);
    dumperOptions.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
    dumperOptions.setIndent(2);
    dumperOptions.setIndicatorIndent(2);
    dumperOptions.setIndentWithIndicator(true);
    dumperOptions.setSplitLines(false);
    dumperOptions.setWidth(MAXIMUM_LINE_WIDTH);

    return new Yaml(new Constructor(loaderOptions), new Representer(dumperOptions), dumperOptions,
        loaderOptions);
  }
}
