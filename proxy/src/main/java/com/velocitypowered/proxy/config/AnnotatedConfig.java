package com.velocitypowered.proxy.config;

import java.io.IOException;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Pattern;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Simple annotation and fields based TOML configuration serializer.
 */
public abstract class AnnotatedConfig {

  private static final Logger logger = LogManager.getLogger(AnnotatedConfig.class);
  private static final Pattern STRING_NEEDS_ESCAPE
      = Pattern.compile("(\"|\\\\|[\\u0000-\\u0008]|[\\u000a-\\u001f]|\\u007f)");

  public static Logger getLogger() {
    return logger;
  }

  /**
   * Indicates that a field is a table.
   */
  @Retention(RetentionPolicy.RUNTIME)
  @Target({ElementType.FIELD, ElementType.TYPE})
  public @interface Table {
    /**
     * The table's name.
     * @return the table's name
     */
    String value();
  }

  /**
   * Creates a comment.
   */
  @Retention(RetentionPolicy.RUNTIME)
  @Target({ElementType.FIELD, ElementType.TYPE})
  public @interface Comment {
    /**
     * The comments to include with this key. Each entry is considered a line.
     * @return the comments
     */
    String[] value();
  }

  /**
   * How field will be named in config.
   */
  @Retention(RetentionPolicy.RUNTIME)
  @Target({ElementType.FIELD, ElementType.TYPE})
  public @interface ConfigKey {
    /**
     * The name of this field in the configuration.
     * @return the field's name
     */
    String value();
  }

  /**
   * Indicates that a field is a map and we need to save all map data to config.
   */
  @Retention(RetentionPolicy.RUNTIME)
  @Target({ElementType.FIELD, ElementType.TYPE})
  public @interface IsMap {

  }

  /**
   * Indicates that a field is a string converted to byte[].
   */
  @Retention(RetentionPolicy.RUNTIME)
  @Target({ElementType.FIELD, ElementType.TYPE})
  public @interface StringAsBytes {

  }

  /**
   * Indicates that a field should be skipped.
   */
  @Retention(RetentionPolicy.RUNTIME)
  @Target({ElementType.FIELD})
  public @interface Ignore {

  }

  /**
   * Dumps this configuration to list of strings using {@link #dumpConfig(Object)}.
   *
   * @return configuration dump
   */
  public List<String> dumpConfig() {
    return dumpConfig(this);
  }

  /**
   * Creates TOML configuration from supplied <pre>dumpable</pre> object.
   *
   * @param dumpable object which is going to be dumped
   * @return string list of configuration file lines
   * @throws RuntimeException if reading field value(s) fail
   */
  private static List<String> dumpConfig(Object dumpable) {
    List<String> lines = new ArrayList<>();
    try {
      for (Field field : dumpable.getClass().getDeclaredFields()) {
        // Skip fields with @Ignore annotation
        if (field.getAnnotation(Ignore.class) != null) {
          continue;
        }

        // Make field accessible
        field.setAccessible(true);

        // Add comments
        Comment comment = field.getAnnotation(Comment.class);
        if (comment != null) {
          for (String line : comment.value()) {
            lines.add("# " + line);
          }
        }

        // Get a key name for config. Use field name if @ConfigKey annotation is not present.
        ConfigKey key = field.getAnnotation(ConfigKey.class);
        final String name = escapeKeyIfNeeded(key == null ? field.getName() : key.value());

        Object value = field.get(dumpable);

        // Check if field is table.
        Table table = field.getAnnotation(Table.class);
        if (table != null) {
          lines.add(table.value()); // Write [name]
          lines.addAll(dumpConfig(value)); // Dump fields of table
          continue;
        }

        if (field.getAnnotation(IsMap.class) != null) { // Check if field is a map
          @SuppressWarnings("unchecked")
          Map<String, ?> map = (Map<String, ?>) value;
          for (Entry<String, ?> entry : map.entrySet()) {
            lines.add(escapeKeyIfNeeded(entry.getKey()) + " = " + serialize(entry.getValue()));
          }
          lines.add(""); // Add empty line
          continue;
        }

        // Check if field is a byte[] representation of a string
        if (field.getAnnotation(StringAsBytes.class) != null) {
          value = new String((byte[]) value, StandardCharsets.UTF_8);
        }

        // Save field to config
        lines.add(name + " = " + serialize(value));
        lines.add(""); // Add empty line
      }
    } catch (IllegalAccessException | IllegalArgumentException | SecurityException e) {
      throw new RuntimeException("Could not dump configuration", e);
    }

    return lines;
  }

  /**
   * Serializes <pre>value</pre> so it can be parsed as a TOML value.
   *
   * @param value object to serialize
   * @return Serialized object
   */
  private static String serialize(Object value) {
    if (value instanceof List) {
      List<?> listValue = (List<?>) value;
      if (listValue.isEmpty()) {
        return "[]";
      }

      StringBuilder m = new StringBuilder();
      m.append("[");

      for (Object obj : listValue) {
        m.append(System.lineSeparator()).append("  ").append(serialize(obj)).append(",");
      }

      m.deleteCharAt(m.length() - 1).append(System.lineSeparator()).append("]");
      return m.toString();
    }

    if (value instanceof Enum) {
      value = value.toString();
    }

    if (value instanceof String) {
      return writeString((String) value);
    }

    return value != null ? value.toString() : "null";
  }

  protected static String escapeKeyIfNeeded(String key) {
    if ((key.contains(".") || key.contains(" "))
        && !(key.indexOf('"') == 0 && key.lastIndexOf('"') == (key.length() - 1))) {
      return '"' + key + '"';
    }
    return key;
  }

  private static String writeString(String str) {
    if (str.isEmpty()) {
      return "\"\"";
    }

    // According to the TOML specification (https://toml.io/en/v1.0.0-rc.1#section-7):
    //
    // Any Unicode character may be used except those that must be escaped: quotation mark,
    // backslash, and the control characters other than tab (U+0000 to U+0008, U+000A to U+001F,
    // U+007F).
    if (STRING_NEEDS_ESCAPE.matcher(str).find()) {
      return "'" + str + "'";
    } else {
      return "\"" + str.replace("\n", "\\n") + "\"";
    }
  }

  protected static String unescapeKeyIfNeeded(String key) {
    int lastIndex;
    if (key.indexOf('"') == 0 && (lastIndex = key.lastIndexOf('"')) == (key.length() - 1)) {
      return key.substring(1, lastIndex);
    }
    return key;
  }

  /**
   * Writes list of strings to file.
   *
   * @param lines list of strings to write
   * @param to Path of file where lines should be written
   * @throws IOException if error occurred during writing
   * @throws IllegalArgumentException if <pre>lines</pre> is empty list
   */
  public static void saveConfig(List<String> lines, Path to) throws IOException {
    if (lines.isEmpty()) {
      throw new IllegalArgumentException("lines cannot be empty");
    }

    Path temp = to.toAbsolutePath().getParent().resolve(to.getFileName().toString() + "__tmp");
    Files.write(temp, lines, StandardCharsets.UTF_8, StandardOpenOption.CREATE);
    try {
      Files.move(temp, to, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
    } catch (AtomicMoveNotSupportedException e) {
      Files.move(temp, to, StandardCopyOption.REPLACE_EXISTING);
    }
  }
}
