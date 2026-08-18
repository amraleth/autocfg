package dev.amraleth.autocfg;

import dev.amraleth.autocfg.annotation.DefaultEntry;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemoryConfiguration;
import org.jetbrains.annotations.Unmodifiable;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.RecordComponent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Configuration mapper that performs the mappings between config <-> class.
 *
 * @author amraleth
 * @since 1.0.0
 */
final class ConfigMapper {

    /**
     * The record types currently being seeded on this thread, guarding against a record that
     * reaches itself through {@link DefaultEntry}.
     */
    private static final ThreadLocal<Set<Class<?>>> SEEDING = ThreadLocal.withInitial(HashSet::new);

    /**
     * Prevents instantiation of this utility class.
     */
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
    static <T extends Record> T read(ConfigurationSection section, Class<T> type) {
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
    static <T extends Record> T read(ConfigurationSection section, Class<T> type, String prefix) {
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
    static Object resolve(ConfigurationSection section, RecordComponent component, String prefix) {
        String path = Components.path(prefix, component);
        Class<?> type = component.getType();
        Components.validateDefaultUse(component);
        if (type.isRecord()) {
            return read(section, type.asSubclass(Record.class), path);
        }

        boolean present = section.contains(path);
        Optional<Class<?>> element = Components.element(component);
        if (type == Optional.class) {
            return optional(section, path, element, present);
        }

        if (type == List.class && element.filter(Class::isRecord).isPresent()) {
            return recordList(section, path, element.orElseThrow(), present, component);
        }

        Optional<List<Object>> defaults = Components.defaults(component);
        Optional<List<String>> legacyDefaults = Components.legacyDefaults(component);
        if (!present && defaults.isEmpty() && legacyDefaults.isEmpty()) {
            throw new IllegalStateException("Missing key %s and no @Default annotation declared".formatted(path));
        }
        try {
            return present
                    ? Values.fromNode(Objects.requireNonNull(section.get(path)), type, element)
                    : defaults.map(values -> Values.fromDefaults(values, type, element))
                    .orElseGet(() -> Values.fromLegacyText(legacyDefaults.orElseThrow(), type, element));
        } catch (RuntimeException exception) {
            throw decode(path, exception);
        }
    }

    /**
     * Resolves an {@link Optional} component. An absent key yields an empty optional; no
     * {@link dev.amraleth.autocfg.annotation.Default} annotation is required.
     *
     * @param section The section to resolve from.
     * @param path    The path of the component.
     * @param element The element type of the optional.
     * @param present Whether the key is present in the section.
     * @return The resolved optional.
     * @throws IllegalStateException If the optional has no resolvable element type.
     */
    private static Optional<?> optional(ConfigurationSection section, String path,
                                        Optional<Class<?>> element, boolean present) {
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
     * <p>An absent key yields an empty list, or a single seeded entry if the component declares
     * {@link DefaultEntry}.
     *
     * @param section   The section to resolve from.
     * @param path      The path of the component.
     * @param element   The record type of the list entries.
     * @param present   Whether the key is present in the section.
     * @param component The component being resolved.
     * @return The resolved list.
     * @throws IllegalStateException If a non-empty default is declared for a record list, or if the
     *                               key is absent and the component declares neither a default nor
     *                               {@link DefaultEntry}.
     */
    private static List<? extends Record> recordList(ConfigurationSection section, String path,
                                                     Class<?> element, boolean present,
                                                     RecordComponent component) {
        if (!present) {
            Optional<List<Object>> defaults = Components.defaults(component);
            Optional<List<String>> legacyDefaults = Components.legacyDefaults(component);
            if (defaults.filter(declared -> !declared.isEmpty()).isPresent()) {
                throw new IllegalStateException(
                        "Record list defaults must be empty; use @DefaultEntry or declare the section %s in the file"
                                .formatted(path));
            }
            if (legacyDefaults.filter(declared -> !declared.isEmpty()).isPresent()) {
                throw new IllegalStateException(
                        "Record list defaults must be empty; use @DefaultEntry or declare the section %s in the file"
                                .formatted(path));
            }
            if (component.isAnnotationPresent(DefaultEntry.class)) {
                return List.of(seed(element.asSubclass(Record.class), path));
            }
            if (defaults.isEmpty() && legacyDefaults.isEmpty()) {
                throw new IllegalStateException(
                        "Missing key %s: declare @Default.Empty for an empty list, or @DefaultEntry to seed one"
                                .formatted(path));
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
    private static <T extends Record> @Unmodifiable List<T> records(List<?> items, Class<T> type) {
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
     * Builds a single record from its declared defaults alone, by reading it out of an empty
     * section: every component falls back to its {@link dev.amraleth.autocfg.annotation.Default} annotation,
     * nested records recurse, and optionals come out empty.
     *
     * <p>The record's compact constructor runs as usual, so a default that fails validation fails
     * the load.
     *
     * @param type The record type to seed.
     * @param path The path of the list being seeded, used for the failure message.
     * @param <T>  The record type to seed.
     * @return The seeded record.
     * @throws IllegalStateException If the record cannot be built without a file, or if seeding
     *                               cycles back into a type already being seeded.
     */
    private static <T extends Record> T seed(Class<T> type, String path) {
        Set<Class<?>> seeding = SEEDING.get();
        if (!seeding.add(type)) {
            throw new IllegalStateException("@DefaultEntry on %s cycles through %s"
                    .formatted(path, type.getSimpleName()));
        }
        try {
            return read(new MemoryConfiguration(), type);
        } catch (RuntimeException exception) {
            throw new IllegalStateException("Cannot seed a default entry for %s: %s"
                    .formatted(path, exception.getMessage()), exception);
        } finally {
            seeding.remove(type);
        }
    }

    /**
     * Adapts a raw list entry into a configuration section.
     *
     * @param item The raw entry.
     * @return The entry as a section.
     * @throws IllegalArgumentException If the entry is not a section.
     */
    private static ConfigurationSection entry(Object item) {
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
    private static IllegalArgumentException decode(String path, RuntimeException exception) {
        return new IllegalArgumentException("Cannot decode %s: %s".formatted(path, exception.getMessage()), exception);
    }

    /**
     * Checks if a list of components have distinct keys.
     *
     * @param components The components.
     * @param prefix     The prefix.
     * @throws IllegalStateException If duplicate keys exist.
     */
    static void checkDistinctKeys(RecordComponent[] components, String prefix) {
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
    static <T extends Record> T instantiate(Class<T> type, RecordComponent[] components, Object[] values) {
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
    private static void open(Constructor<?> constructor, Class<?> type) {
        try {
            constructor.setAccessible(true);
        } catch (RuntimeException exception) {
            throw new IllegalStateException(
                    "Cannot access the constructor of %s; make the record public or open its module to %s"
                            .formatted(type.getSimpleName(), ConfigMapper.class.getModule().getName()), exception);
        }
    }
}
