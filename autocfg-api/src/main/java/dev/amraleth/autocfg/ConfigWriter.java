package dev.amraleth.autocfg;

import org.bukkit.configuration.ConfigurationSection;
import org.jspecify.annotations.NonNull;

import java.lang.reflect.RecordComponent;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Facilitates writes to the configuration.
 *
 * @author amraleth
 * @since 1.0.0
 */
final class ConfigWriter {

    private ConfigWriter() {
    }

    /**
     * Writes a section.
     *
     * @param section The section to write to.
     * @param source  The source to write from.
     */
    static void write(@NonNull ConfigurationSection section, @NonNull Record source) {
        write(section, source, "");
    }

    /**
     * Writes a section with a given prefix.
     *
     * @param section The section to write to.
     * @param source  The source to write from.
     * @param prefix  The prefix to use.
     */
    static void write(@NonNull ConfigurationSection section, @NonNull Record source, @NonNull String prefix) {
        Arrays.stream(source.getClass().getRecordComponents())
                .forEach(component -> writeComponent(section, component, source, prefix));
    }

    /**
     * Writes a component.
     *
     * @param section   The section to write to.
     * @param component The component to write from.
     * @param source    The source record.
     * @param prefix    The prefix to use.
     */
    static void writeComponent(@NonNull ConfigurationSection section, @NonNull RecordComponent component,
                               @NonNull Record source, @NonNull String prefix) {
        String path = Components.path(prefix, component);
        Object value = Components.value(component, source);
        if (component.getType().isRecord()) {
            section(section, path);
            write(section, (Record) value, path);
        } else if (value instanceof Optional<?> optional) {
            writeOptional(section, path, optional);
        } else {
            section.set(path, node(value));
        }
        comment(section, path, component);
    }

    /**
     * Writes an optional component. An empty optional is omitted from the file, clearing any value
     * a previous write left behind.
     *
     * @param section The section to write to.
     * @param path    The path to write to.
     * @param value   The optional value.
     */
    private static void writeOptional(@NonNull ConfigurationSection section, @NonNull String path, @NonNull Optional<?> value) {
        Object inner = value.orElse(null);
        if (inner == null) {
            section.set(path, null);
        } else if (inner instanceof Record record) {
            section(section, path);
            write(section, record, path);
        } else {
            section.set(path, node(inner));
        }
    }

    /**
     * Ensures a section exists at a path, leaving an existing section and its contents in place.
     *
     * @param section The section to write to.
     * @param path    The path the section should exist at.
     */
    private static void section(@NonNull ConfigurationSection section, @NonNull String path) {
        if (!section.isConfigurationSection(path)) {
            section.createSection(path);
        }
    }

    /**
     * Converts a value to a node, expanding records into maps so they can be nested inside lists.
     *
     * @param value The value.
     * @return The node.
     */
    private static @NonNull Object node(@NonNull Object value) {
        if (value instanceof Record record) {
            return toMap(record);
        }
        if (value instanceof List<?> list) {
            return list.stream().map(ConfigWriter::node).toList();
        }
        return Values.toNode(value);
    }

    /**
     * Converts a record to a map, in component declaration order.
     *
     * <p>Applies to records nested inside lists. Bukkit attaches no comments to entries inside a
     * list, so {@code @ConfigComment} on such a record's components has no effect.
     *
     * @param source The record to convert.
     * @return The record as a map.
     */
    private static @NonNull Map<String, Object> toMap(@NonNull Record source) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (RecordComponent component : source.getClass().getRecordComponents()) {
            String key = Components.path("", component);
            Object value = Components.value(component, source);
            if (value instanceof Optional<?> optional) {
                optional.ifPresent(inner -> map.put(key, node(inner)));
            } else {
                map.put(key, node(value));
            }
        }
        return map;
    }

    /**
     * Writes a comment.
     *
     * @param section   The section to write to.
     * @param path      The path to use.
     * @param component The component to write from.
     */
    static void comment(@NonNull ConfigurationSection section, @NonNull String path, @NonNull RecordComponent component) {
        if (!section.contains(path)) {
            return;
        }
        List<String> declared = Components.comments(component);
        List<String> comments = declared.isEmpty() && component.getType().isRecord()
                ? Components.comments(component.getType())
                : declared;
        if (!comments.isEmpty()) {
            section.setComments(path, comments);
        }
    }
}
