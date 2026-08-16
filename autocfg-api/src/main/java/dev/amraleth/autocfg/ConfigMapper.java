package dev.amraleth.autocfg;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemoryConfiguration;
import org.jspecify.annotations.NonNull;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.RecordComponent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Configuration mapper that performs the mappings between config <-> class.
 *
 * @author amraleth
 * @since 1.0.0
 */
final class ConfigMapper {

    private ConfigMapper() {
    }

    /**
     * Reads into a record from a configuration section.
     *
     * @param section The section to read from.
     * @param type    The type class to read.
     * @param <T>     The type of the type class.
     * @return An instance of the type.
     */
    static <T extends Record> @NonNull T read(@NonNull ConfigurationSection section, @NonNull Class<T> type) {
        return read(section, type, "");
    }

    /**
     * Reads into a record from a section with a given prefix.
     *
     * @param section The section to read from.
     * @param type    The type class to read.
     * @param prefix  The prefix to read.
     * @param <T>     The type of the type class.
     * @return An instance of the type.
     */
    static <T extends Record> @NonNull T read(@NonNull ConfigurationSection section, @NonNull Class<T> type, @NonNull String prefix) {
        RecordComponent[] components = type.getRecordComponents();
        checkDistinctKeys(components, prefix);
        Object[] values = Arrays.stream(components)
                .map(component -> resolve(section, component, prefix))
                .toArray();
        return instantiate(type, components, values);
    }

    /**
     * Resolves an object from a section and component.
     *
     * @param section   The section to resolve from.
     * @param component The component to resolve.
     * @param prefix    The prefix to resolve from.
     * @return The resolved object.
     */
    static @NonNull Object resolve(@NonNull ConfigurationSection section, @NonNull RecordComponent component, @NonNull String prefix) {
        String path = Components.path(prefix, component);
        Class<?> type = component.getType();
        if (type.isRecord()) {
            return read(section, type.asSubclass(Record.class), path);
        }

        boolean present = section.contains(path);
        Optional<Class<?>> element = Components.element(component);
        if (type == Optional.class) {
            return optional(section, path, element, present);
        }

        Optional<List<String>> literals = Components.defaults(component);
        if (!present && literals.isEmpty()) {
            throw new IllegalStateException("Missing key %s and no @DefaultValue declared".formatted(path));
        }
        if (type == List.class && element.filter(Class::isRecord).isPresent()) {
            return recordList(section, path, element.orElseThrow(), present, literals);
        }
        try {
            return present
                    ? Values.fromNode(Objects.requireNonNull(section.get(path)), type, element)
                    : Values.fromText(literals.orElseThrow(), type, element);
        } catch (RuntimeException exception) {
            throw decode(path, exception);
        }
    }

    /**
     * Resolves an {@link Optional} component. An absent key yields an empty optional; no
     * {@link dev.amraleth.autocfg.annotation.DefaultValue} is required.
     *
     * @param section The section to resolve from.
     * @param path    The path of the component.
     * @param element The element type of the optional.
     * @param present Whether the key is present in the section.
     * @return The resolved optional.
     * @throws IllegalStateException If the optional has no resolvable element type.
     */
    private static @NonNull Optional<?> optional(@NonNull ConfigurationSection section, @NonNull String path,
                                                 @NonNull Optional<Class<?>> element, boolean present) {
        Class<?> inner = element.orElseThrow(() -> new IllegalStateException(
                "Optional component %s has no resolvable element type".formatted(path)));
        if (!present) {
            return Optional.empty();
        }
        if (inner.isRecord()) {
            return Optional.of(read(section, inner.asSubclass(Record.class), path));
        }
        try {
            return Optional.of(Values.fromNode(Objects.requireNonNull(section.get(path)), inner, Optional.empty()));
        } catch (RuntimeException exception) {
            throw decode(path, exception);
        }
    }

    /**
     * Resolves a list of records. Each entry of the list is treated as its own section and read
     * through {@link #read(ConfigurationSection, Class)}.
     *
     * @param section  The section to resolve from.
     * @param path     The path of the component.
     * @param element  The record type of the list entries.
     * @param present  Whether the key is present in the section.
     * @param literals The declared default literals, if any.
     * @return The resolved list.
     * @throws IllegalStateException If a non-empty default is declared for a record list.
     */
    private static @NonNull List<? extends Record> recordList(@NonNull ConfigurationSection section, @NonNull String path,
                                                              @NonNull Class<?> element, boolean present,
                                                              @NonNull Optional<List<String>> literals) {
        if (!present) {
            if (!literals.orElseThrow().isEmpty()) {
                throw new IllegalStateException(
                        "Record list defaults must be empty; declare the section %s in the file instead".formatted(path));
            }
            return List.of();
        }
        Object node = Objects.requireNonNull(section.get(path));
        try {
            if (!(node instanceof List<?> items)) {
                throw new IllegalArgumentException("Expected a list, got %s (%s)"
                        .formatted(node, node.getClass().getSimpleName()));
            }
            return records(items, element.asSubclass(Record.class));
        } catch (RuntimeException exception) {
            throw decode(path, exception);
        }
    }

    /**
     * Reads each entry of a list as a record.
     *
     * @param items The raw list entries.
     * @param type  The record type of the entries.
     * @param <T>   The record type of the entries.
     * @return The read records.
     * @throws IllegalArgumentException If an entry is not a section.
     */
    private static <T extends Record> @NonNull List<T> records(@NonNull List<?> items, @NonNull Class<T> type) {
        List<T> records = new ArrayList<>(items.size());
        for (int index = 0; index < items.size(); index++) {
            Object item = items.get(index);
            try {
                records.add(read(entry(item), type));
            } catch (RuntimeException exception) {
                throw new IllegalArgumentException("entry %d: %s".formatted(index, exception.getMessage()), exception);
            }
        }
        return List.copyOf(records);
    }

    /**
     * Adapts a raw list entry into a configuration section.
     *
     * @param item The raw entry.
     * @return The entry as a section.
     * @throws IllegalArgumentException If the entry is not a section.
     */
    private static @NonNull ConfigurationSection entry(Object item) {
        if (item instanceof ConfigurationSection section) {
            return section;
        }
        if (item instanceof Map<?, ?> map) {
            return new MemoryConfiguration().createSection("entry", map);
        }
        throw new IllegalArgumentException("Expected a section, got %s (%s)"
                .formatted(item, item == null ? "null" : item.getClass().getSimpleName()));
    }

    /**
     * Wraps a decoding failure with the offending key.
     *
     * @param path      The path that failed to decode.
     * @param exception The underlying failure.
     * @return The wrapped exception.
     */
    private static @NonNull IllegalArgumentException decode(@NonNull String path, @NonNull RuntimeException exception) {
        return new IllegalArgumentException("Cannot decode %s: %s".formatted(path, exception.getMessage()), exception);
    }

    /**
     * Checks if a list of components have distinct keys.
     *
     * @param components The components.
     * @param prefix     The prefix.
     * @throws IllegalStateException If duplicate keys exist.
     */
    static void checkDistinctKeys(@NonNull RecordComponent[] components, @NonNull String prefix) {
        Arrays.stream(components)
                .collect(Collectors.groupingBy(component -> Components.path(prefix, component)))
                .entrySet().stream()
                .filter(entry -> entry.getValue().size() > 1)
                .findFirst()
                .ifPresent(entry -> {
                    throw new IllegalStateException("Duplicate config key %s".formatted(entry.getKey()));
                });
    }

    /**
     * Instantiates a record.
     *
     * @param type       The type class of record to instantiate.
     * @param components The components.
     * @param values     The values.
     * @param <T>        The type of the type class.
     * @return An instance of the record.
     * @throws IllegalStateException If a field cannot be constructed.
     */
    static <T extends Record> @NonNull T instantiate(@NonNull Class<T> type, @NonNull RecordComponent[] components, @NonNull Object[] values) {
        Class<?>[] parameters = Arrays.stream(components)
                .map(RecordComponent::getType)
                .toArray(Class<?>[]::new);

        try {
            Constructor<T> constructor = type.getDeclaredConstructor(parameters);
            open(constructor, type);
            return constructor.newInstance(values);
        } catch (InvocationTargetException exception) {
            throw exception.getCause() instanceof RuntimeException validation
                    ? validation
                    : new IllegalStateException("Cannot construct %s".formatted(type.getSimpleName()), exception.getCause());
        } catch (ReflectiveOperationException | IllegalArgumentException exception) {
            throw new IllegalStateException("Cannot construct %s: %s"
                    .formatted(type.getSimpleName(), exception), exception);
        }
    }

    /**
     * Makes a canonical constructor invocable.
     *
     * <p>A record declared without {@code public} has a package-private canonical constructor, which
     * is not accessible from this package.
     *
     * @param constructor The constructor to open.
     * @param type        The record type, used for the failure message.
     * @throws IllegalStateException If access cannot be granted.
     */
    private static void open(@NonNull Constructor<?> constructor, @NonNull Class<?> type) {
        try {
            constructor.setAccessible(true);
        } catch (RuntimeException exception) {
            throw new IllegalStateException(
                    "Cannot access the constructor of %s; make the record public or open its module to %s"
                            .formatted(type.getSimpleName(), ConfigMapper.class.getModule().getName()), exception);
        }
    }
}
