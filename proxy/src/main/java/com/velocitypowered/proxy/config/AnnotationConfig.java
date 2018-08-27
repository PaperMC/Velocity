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
public class AnnotationConfig {

    private static final Logger logger = LogManager.getLogger(AnnotationConfig.class);

    public static Logger getLogger() {
        return logger;
    }

    /**
     * Indicates that a field is a table
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.FIELD})
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
    public @interface CfgKey {

        String value();
    }

    /**
     * Indicates that a field is map and we need to save all data to config
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.FIELD, ElementType.TYPE})
    public @interface AsMap {
    }

    /**
     * Indicates that a field is a string converted to byte[]
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.FIELD, ElementType.TYPE})
    public @interface AsBytes {
    }

    /**
     * Indicates that a field is a string converted to byte[]
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.FIELD, ElementType.TYPE})
    public @interface Ignore {
    }

    public List<String> dumpConfig() {
        List<String> lines = new ArrayList<>();
        dumpFields(getClass(), this, lines);
        return lines;
    }

    private void dumpFields(Class root, Object caller, List<String> lines) {

        try {
            for (Field field : root.getDeclaredFields()) {
                if (field.getAnnotation(Ignore.class) != null) {
                    continue;
                }
                Comment comment = field.getAnnotation(Comment.class);
                if (comment != null) { //Add comments
                    for (String line : comment.value()) {
                        lines.add("# " + line);
                    }
                }
                CfgKey key = field.getAnnotation(CfgKey.class);
                String name = key == null ? field.getName() : key.value();
                field.setAccessible(true);
                Table table = field.getAnnotation(Table.class);
                if (table != null) {
                    lines.add(table.value()); // Write [name]
                    dumpFields(field.getType(), field.get(caller), lines); // dump a table class
                } else {
                    if (field.getAnnotation(AsMap.class) != null) {
                        Map<String, ?> map = (Map<String, ?>) field.get(caller);
                        for (Entry<String, ?> entry : map.entrySet()) {
                            lines.add(entry.getKey() + " = " + toString(entry.getValue()));
                        }
                        lines.add("");
                        continue;
                    }
                    Object value = field.get(caller);
                    if (field.getAnnotation(AsBytes.class) != null) {
                        value = new String((byte[]) value, StandardCharsets.UTF_8);
                    }
                    lines.add(name + " = " + toString(value));
                    lines.add("");
                }
            }
        } catch (IllegalAccessException | IllegalArgumentException | SecurityException e) {
            logger.log(Level.ERROR, "Unexpected error while dumping fields", e);
            lines.clear();
        }
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
