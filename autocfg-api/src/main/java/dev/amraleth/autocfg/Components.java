package dev.amraleth.autocfg;

import dev.amraleth.autocfg.annotation.ConfigComment;
import dev.amraleth.autocfg.annotation.ConfigKey;
import dev.amraleth.autocfg.annotation.DefaultValue;
import org.jspecify.annotations.NonNull;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.RecordComponent;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

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
    static @NonNull Optional<List<String>> defaults(@NonNull RecordComponent component) {
        return Optional.ofNullable(component.getAnnotation(DefaultValue.class))
                .map(annotation -> List.of(annotation.value()));
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
     *         itself generic.
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
