package com.velocitypowered.proxy.config;

import java.io.File;
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
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Only for simple configs
 */
public class AnnotatedConfig {

    private static final Logger logger = LogManager.getLogger(AnnotatedConfig.class);

    public static Logger getLogger() {
        return logger;
    }

    /**
     * Indicates that a field is a table
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.FIELD, ElementType.TYPE})
    public @interface Table {

        String value();
    }

    /**
     * Creates a comment
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.FIELD, ElementType.TYPE})
    public @interface Comment {

        String[] value();
    }

    /**
     * How field will be named in config
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.FIELD, ElementType.TYPE})
    public @interface ConfigKey {

        String value();
    }

    /**
     * Indicates that a field is a map and we need to save all map data to
     * config
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.FIELD, ElementType.TYPE})
    public @interface IsMap {
    }

    /**
     * Indicates that a field is a string converted to byte[]
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.FIELD, ElementType.TYPE})
    public @interface StringAsBytes {
    }

    /**
     * Indicates that a field should be skiped
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.FIELD})
    public @interface Ignore {
    }

    public List<String> dumpConfig() {
        List<String> lines = new ArrayList<>();
        if (!dumpFields(this, lines)) {
            throw new RuntimeException("can not dump config");
        }
        return lines;
    }

    /**
     * Dump all field and they annotations to List
     *
     * @param toSave object those we need to dump
     * @param lines a list where store dumped lines
     */
    private boolean dumpFields(Object toSave, List<String> lines) {

        try {
            for (Field field : toSave.getClass().getDeclaredFields()) {
                if (field.getAnnotation(Ignore.class) != null) { //Skip this field
                    continue;
                }
                Comment comment = field.getAnnotation(Comment.class);
                if (comment != null) { //Add comments
                    for (String line : comment.value()) {
                        lines.add("# " + line);
                    }
                }
                ConfigKey key = field.getAnnotation(ConfigKey.class); //Get a key name for config
                String name = key == null ? field.getName() : key.value(); // Use a field name if name in annotation is not present
                field.setAccessible(true); // Make field accessible
                Table table = field.getAnnotation(Table.class);
                if (table != null) { // Check if field is table.
                    lines.add(table.value()); // Write [name] 
                    dumpFields(field.get(toSave), lines); // dump fields of table class
                } else {
                    if (field.getAnnotation(IsMap.class) != null) { // check if field is map
                        Map<String, ?> map = (Map<String, ?>) field.get(toSave);
                        for (Entry<String, ?> entry : map.entrySet()) {
                            lines.add(entry.getKey() + " = " + toString(entry.getValue())); // Save a map data
                        }
                        lines.add(""); //Add empty line
                        continue;
                    }
                    Object value = field.get(toSave);
                    if (field.getAnnotation(StringAsBytes.class) != null) { // Check if field is a byte[] representation of  a string
                        value = new String((byte[]) value, StandardCharsets.UTF_8);
                    }
                    lines.add(name + " = " + toString(value)); // save field to config
                    lines.add(""); // add empty line
                }
            }
        } catch (IllegalAccessException | IllegalArgumentException | SecurityException e) {
            logger.log(Level.ERROR, "Unexpected error while dumping fields", e);
            lines.clear();
            return false;
        }
        return true;
    }

    private String toString(Object value) {
        if (value instanceof List) {
            Collection<?> listValue = (Collection<?>) value;
            if (listValue.isEmpty()) {
                return "[]";
            }
            StringBuilder m = new StringBuilder();
            m.append("[");
            for (Object obj : listValue) {
                m.append(System.lineSeparator()).append("  ").append(toString(obj)).append(",");
            }
            m.deleteCharAt(m.length() - 1).append(System.lineSeparator()).append("]");
            return m.toString();
        }
        if (value instanceof Enum) {
            value = value.toString();
        }
        if (value instanceof String) {
            String stringValue = (String) value;
            if (stringValue.isEmpty()) {
                return "\"\"";
            }
            return "\"" + stringValue + "\"";
        }
        return value != null ? value.toString() : "null";
    }

    /**
     * Saves lines to file
     *
     * @param lines Lines to save
     * @param to A path of file where to save lines
     * @throws IOException if lines is empty or was error during saving
     */
    public static void saveConfig(List<String> lines, Path to) throws IOException {
        if (lines.isEmpty()) {
            throw new IOException("Can not save config because list is empty");
        }
        Path temp = new File(to.toFile().getParent(), "__tmp").toPath();
        Files.write(temp, lines, StandardCharsets.UTF_8, StandardOpenOption.CREATE);
        try {
            Files.move(temp, to, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(temp, to, StandardCopyOption.REPLACE_EXISTING);
        }

    }
}
