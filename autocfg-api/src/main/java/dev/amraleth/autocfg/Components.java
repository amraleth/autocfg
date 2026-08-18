package dev.amraleth.autocfg;

import dev.amraleth.autocfg.annotation.ConfigComment;
import dev.amraleth.autocfg.annotation.ConfigKey;
import dev.amraleth.autocfg.annotation.Default;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.jspecify.annotations.NonNull;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.RecordComponent;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Function;

/**
 * Private helpers for working with configuration components.
 *
 * @author amraleth
 * @since 1.0.0
 */
final class Components {

    private Components() {
    }

    /**
     * Constructs a yml path from a prefix and a component.
     *
     * @param prefix    The prefix of the path.
     * @param component The component to generate the path for.
     * @return The path as a string. If the prefix is empty, the component will be treated as the root key.
     */
    static @NonNull String path(@NonNull String prefix, @NonNull RecordComponent component) {
        String key = Optional.ofNullable(component.getAnnotation(ConfigKey.class))
                .map(annotation -> key(annotation.value(), component))
                .orElseGet(() -> kebab(component.getName()));
        return prefix.isEmpty() ? key : "%s.%s".formatted(prefix, key);
    }

    /**
     * Validates a declared configuration key.
     *
     * @param key       The declared key.
     * @param component The component the key was declared on.
     * @return The key.
     * @throws IllegalStateException If the key is blank or contains {@code '.'}.
     */
    private static @NonNull String key(@NonNull String key, @NonNull RecordComponent component) {
        if (key.isBlank()) {
            throw new IllegalStateException("@ConfigKey on %s is blank".formatted(component.getName()));
        }
        if (key.indexOf('.') >= 0) {
            throw new IllegalStateException("@ConfigKey on %s must not contain '.', got %s"
                    .formatted(component.getName(), key));
        }
        return key;
    }

    /**
     * Converts a name to <a href="https://developer.mozilla.org/en-US/docs/Glossary/Kebab_case">Kebab case</a>.
     *
     * <p>An acronym is split at its trailing boundary: {@code maxHTTPRetries} yields
     * {@code max-http-retries}.
     *
     * @param name The name to convert.
     * @return The name converted to kebab case.
     */
    static @NonNull String kebab(@NonNull String name) {
        return name.replaceAll("([A-Z]+)([A-Z][a-z])", "$1-$2")
                .replaceAll("([a-z0-9])([A-Z])", "$1-$2")
                .toLowerCase(Locale.ROOT);
    }

    /**
     * Fetches the default values for a given component.
     *
     * @param component The component.
     * @return A list of all default values.
     */
    static @NonNull Optional<List<Object>> defaults(@NonNull RecordComponent component) {
        List<DefaultValues> declared = new ArrayList<>();
        add(declared, component.getAnnotation(Default.Boolean.class), boolean.class, Default.Boolean::value, "@Default.Boolean");
        add(declared, component.getAnnotation(Default.Byte.class), byte.class, Default.Byte::value, "@Default.Byte");
        add(declared, component.getAnnotation(Default.Short.class), short.class, Default.Short::value, "@Default.Short");
        add(declared, component.getAnnotation(Default.Integer.class), int.class, Default.Integer::value, "@Default.Integer");
        add(declared, component.getAnnotation(Default.Long.class), long.class, Default.Long::value, "@Default.Long");
        add(declared, component.getAnnotation(Default.Float.class), float.class, Default.Float::value, "@Default.Float");
        add(declared, component.getAnnotation(Default.Double.class), double.class, Default.Double::value, "@Default.Double");
        add(declared, component.getAnnotation(Default.Character.class), char.class, Default.Character::value, "@Default.Character");
        add(declared, component.getAnnotation(Default.String.class), String.class, Default.String::value, "@Default.String");
        add(declared, component.getAnnotation(Default.Enum.class), Enum.class, Default.Enum::value, "@Default.Enum");
        add(declared, component.getAnnotation(Default.Duration.class), java.time.Duration.class, Default.Duration::value, "@Default.Duration");
        add(declared, component.getAnnotation(Default.NamespacedKey.class), NamespacedKey.class, Default.NamespacedKey::value, "@Default.NamespacedKey");
        add(declared, component.getAnnotation(Default.Material.class), Material.class, Default.Material::value, "@Default.Material");
        add(declared, component.getAnnotation(Default.Text.class), null, Default.Text::value, "@Default.Text");
        if (component.isAnnotationPresent(Default.Empty.class)) {
            declared.add(new DefaultValues(null, List.of(), "@Default.Empty"));
        }
        if (declared.size() > 1) {
            throw new IllegalStateException("Multiple defaults declared on %s".formatted(component.getName()));
        }
        if (declared.isEmpty()) {
            return Optional.empty();
        }
        DefaultValues defaults = declared.getFirst();
        validate(component, defaults);
        return Optional.of(defaults.values());
    }

    private static <A extends java.lang.annotation.Annotation> void add(@NonNull List<DefaultValues> defaults,
                                                                        A annotation, Class<?> type,
                                                                        Function<A, Object> values, String name) {
        if (annotation != null) {
            defaults.add(new DefaultValues(type, array(values.apply(annotation)), name));
        }
    }

    private static @NonNull List<Object> array(@NonNull Object array) {
        List<Object> values = new ArrayList<>(Array.getLength(array));
        for (int index = 0; index < Array.getLength(array); index++) {
            values.add(Array.get(array, index));
        }
        return List.copyOf(values);
    }

    private static void validate(@NonNull RecordComponent component, @NonNull DefaultValues defaults) {
        if ("@Default.Empty".equals(defaults.name())) {
            if (component.getType() != List.class || element(component).filter(Class::isRecord).isEmpty()) {
                throw new IllegalStateException("@Default.Empty on %s requires List<SomeRecord>"
                        .formatted(component.getName()));
            }
            return;
        }
        Class<?> actual = component.getType() == List.class
                ? element(component).orElseThrow(() -> new IllegalStateException(
                "List component %s has no resolvable element type".formatted(component.getName())))
                : component.getType();
        Class<?> expected = defaults.type();
        if (expected == null && actual.isRecord()) {
            throw new IllegalStateException("%s on %s cannot default a record"
                    .formatted(defaults.name(), component.getName()));
        }
        if (expected == null || (expected == Enum.class && actual.isEnum()) || boxed(expected) == boxed(actual)) {
            return;
        }
        throw new IllegalStateException("%s on %s does not match %s"
                .formatted(defaults.name(), component.getName(), actual.getSimpleName()));
    }

    private static @NonNull Class<?> boxed(@NonNull Class<?> type) {
        return switch (type.getName()) {
            case "boolean" -> Boolean.class;
            case "byte" -> Byte.class;
            case "short" -> Short.class;
            case "int" -> Integer.class;
            case "long" -> Long.class;
            case "float" -> Float.class;
            case "double" -> Double.class;
            case "char" -> Character.class;
            default -> type;
        };
    }

    private record DefaultValues(Class<?> type, List<Object> values, String name) {
    }

    /**
     * Fetches the value for a given component.
     *
     * @param component The component.
     * @param owner     The owner.
     * @return The value.
     * @throws IllegalStateException if the component cannot be read.
     */
    static @NonNull Object value(@NonNull RecordComponent component, @NonNull Record owner) {
        try {
            Method accessor = component.getAccessor();
            accessor.setAccessible(true);
            return accessor.invoke(owner);
        } catch (ReflectiveOperationException | RuntimeException exception) {
            throw new IllegalStateException("Cannot read component %s: %s"
                    .formatted(component.getName(), exception), exception);
        }
    }

    /**
     * Fetches the element class of a component, that is the single type argument of a generic type
     * such as {@code List<Material>} or {@code Optional<String>}.
     *
     * @param component The component.
     * @return An optional class. Empty if the component is not generic, or if its type argument is
     * itself generic.
     */
    static @NonNull Optional<Class<?>> element(@NonNull RecordComponent component) {
        return component.getGenericType() instanceof ParameterizedType type
                && type.getActualTypeArguments().length == 1
                && type.getActualTypeArguments()[0] instanceof Class<?> aType
                ? Optional.of(aType)
                : Optional.empty();
    }

    /**
     * Fetches all comments for a given element.
     *
     * @param element The element.
     * @return A list of all comments.
     */
    static @NonNull List<String> comments(@NonNull AnnotatedElement element) {
        return Optional.ofNullable(element.getAnnotation(ConfigComment.class))
                .map(comment -> List.of(comment.value()))
                .orElseGet(List::of);
    }

}
