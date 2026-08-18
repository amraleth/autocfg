package dev.amraleth.autocfg;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.jetbrains.annotations.Unmodifiable;
import org.jspecify.annotations.NonNull;

/**
 * Helper functions for dealing with values.
 *
 * @author amraleth
 * @since 1.0.0
 */
final class Values {

    /**
     * The box type of each primitive.
     */
    private static final @NonNull @Unmodifiable Map<Class<?>, Class<?>> BOXES = Map.of(
            int.class, Integer.class,
            long.class, Long.class,
            double.class, Double.class,
            float.class, Float.class,
            short.class, Short.class,
            byte.class, Byte.class,
            boolean.class, Boolean.class,
            char.class, Character.class);

    private Values() {
    }

    /**
     * Converts a node to a value.
     *
     * @param node    The node to convert.
     * @param target  The target class to convert to.
     * @param element The element to convert.
     * @return The value.
     * @throws IllegalArgumentException If the node cannot be represented as the target type.
     */
    static @NonNull Object fromNode(@NonNull Object node, @NonNull Class<?> target, @NonNull Optional<Class<?>> element) {
        if (target != List.class) {
            return scalar(node, target);
        }
        if (!(node instanceof List<?> list)) {
            throw new IllegalArgumentException("Expected a list, got %s (%s)"
                    .formatted(node, node.getClass().getSimpleName()));
        }
        Class<?> type = element(element);
        return list.stream().map(item -> scalar(item, type)).toList();
    }

    /**
     * Converts text to a value.
     *
     * @param literals The literals to convert.
     * @param target   The target class to convert.
     * @param element  The element to convert.
     * @return The value.
     * @throws IllegalArgumentException If more than one default value is supplied and the target is not a list.
     */
    static @NonNull Object fromText(@NonNull @Unmodifiable List<String> literals, @NonNull Class<?> target,
                                    @NonNull Optional<Class<?>> element) {
        if (target == List.class) {
            return literals.stream().map(literal -> parse(literal, element(element))).toList();
        }
        if (literals.size() != 1) {
            throw new IllegalArgumentException("Expected a single default literal, got " + literals.size());
        }
        return parse(literals.getFirst(), target);
    }

    /**
     * Converts a value to a node.
     *
     * @param value The value.
     * @return The node.
     */
    static @NonNull Object toNode(@NonNull Object value) {
        if (value instanceof List<?> list) {
            return list.stream().map(Values::toNode).toList();
        }
        return Optional.ofNullable(ConfigConverters.converter(declaring(value)))
                .map(converter -> converter.toNode().apply(value))
                .orElseGet(() -> value instanceof Enum<?> constant
                        ? constant.name().toLowerCase(Locale.ROOT)
                        : value);
    }

    /**
     * Gets the declaring class.
     *
     * @param value The value.
     * @return The declaring class.
     */
    private static @NonNull Class<?> declaring(@NonNull Object value) {
        return value instanceof Enum<?> constant ? constant.getDeclaringClass() : value.getClass();
    }

    /**
     * Gets the element class from an optional element class.
     *
     * @param element The optional element class.
     * @return The element class.
     * @throws IllegalStateException If the element is a list component and has no resolvable element types.
     */
    private static @NonNull Class<?> element(@NonNull Optional<Class<?>> element) {
        return element.orElseThrow(() ->
                new IllegalStateException("List component has no resolvable element type"));
    }

    /**
     * Converts a single node to a value of the target type.
     *
     * <p>A node that cannot be represented as the target type is rejected here, while the offending
     * key is still in scope.
     *
     * @param node   The node to convert.
     * @param target The target class to convert to.
     * @return The value.
     * @throws IllegalArgumentException If the node cannot be represented as the target type.
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    private static @NonNull Object scalar(@NonNull Object node, @NonNull Class<?> target) {
        if (target == char.class || target == Character.class) {
            return character(node.toString());
        }
        ConfigConverters.Converter converter = ConfigConverters.converter(target);
        if (converter != null) {
            return converter.fromNode().apply(node);
        }
        if (target.isEnum()) {
            return constant((Class<Enum>) target, node.toString());
        }
        Class<?> boxed = box(target);
        if (node instanceof Number number && Number.class.isAssignableFrom(boxed)) {
            return widen(number, target);
        }
        if (boxed.isInstance(node)) {
            return node;
        }
        throw new IllegalArgumentException("Expected %s, got %s (%s)"
                .formatted(target.getSimpleName(), node, node.getClass().getSimpleName()));
    }

    /**
     * Gets the box type of primitive, or the type itself if it is not a primitive.
     *
     * @param target The target class.
     * @return The boxed class.
     */
    private static @NonNull Class<?> box(@NonNull Class<?> target) {
        return BOXES.getOrDefault(target, target);
    }

    /**
     * Fetches a constant enum value.
     *
     * @param target  The target enum.
     * @param literal The literal to fetch.
     * @return The constant element.
     * @throws IllegalArgumentException If the literal is not part of the enum.
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    private static @NonNull Object constant(@NonNull Class<Enum> target, @NonNull String literal) {
        try {
            return Enum.valueOf(target, literal.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Expected one of %s, got %s"
                    .formatted(names(target), literal), exception);
        }
    }

    /**
     * Fetches the top 20 names of an enum.
     *
     * @param target The target enum.
     * @return A names concatenated to a list.
     */
    private static @NonNull String names(@NonNull Class<?> target) {
        return Arrays.stream(target.getEnumConstants())
                .limit(20)
                .map(constant -> ((Enum<?>) constant).name().toLowerCase(Locale.ROOT))
                .collect(Collectors.joining(", "));
    }

    /**
     * Parses a primitive literal.
     *
     * @param literal The literal.
     * @param target  The target to parse to.
     * @return The primitive.
     */
    private static @NonNull Object parse(@NonNull String literal, @NonNull Class<?> target) {
        if (target == int.class || target == Integer.class) {
            return Integer.parseInt(literal);
        }
        if (target == long.class || target == Long.class) {
            return Long.parseLong(literal);
        }
        if (target == double.class || target == Double.class) {
            return Double.parseDouble(literal);
        }
        if (target == float.class || target == Float.class) {
            return Float.parseFloat(literal);
        }
        if (target == short.class || target == Short.class) {
            return Short.parseShort(literal);
        }
        if (target == byte.class || target == Byte.class) {
            return Byte.parseByte(literal);
        }
        if (target == boolean.class || target == Boolean.class) {
            return bool(literal);
        }
        return scalar(literal, target);
    }

    /**
     * Parses a single character.
     *
     * @param literal The literal to parse.
     * @return The character.
     * @throws IllegalArgumentException If the literal does not contain exactly one character.
     */
    private static char character(@NonNull String literal) {
        if (literal.length() != 1) {
            throw new IllegalArgumentException("Expected a single character, got " + literal);
        }
        return literal.charAt(0);
    }

    /**
     * Parses a boolean.
     *
     * @param literal The literal to parse.
     * @return The boolean.
     * @throws IllegalArgumentException If the value is neither true nor false.
     */
    private static boolean bool(@NonNull String literal) {
        if ("true".equalsIgnoreCase(literal)) {
            return true;
        }
        if ("false".equalsIgnoreCase(literal)) {
            return false;
        }
        throw new IllegalArgumentException("Expected true or false, got " + literal);
    }

    /**
     * Widens a numeric value.
     *
     * @param value  The value.
     * @param target The target to widen to.
     * @return The widened value.
     */
    private static @NonNull Object widen(@NonNull Number value, @NonNull Class<?> target) {
        if (target == int.class || target == Integer.class) {
            return value.intValue();
        }
        if (target == long.class || target == Long.class) {
            return value.longValue();
        }
        if (target == double.class || target == Double.class) {
            return value.doubleValue();
        }
        if (target == float.class || target == Float.class) {
            return value.floatValue();
        }
        if (target == short.class || target == Short.class) {
            return value.shortValue();
        }
        if (target == byte.class || target == Byte.class) {
            return value.byteValue();
        }
        return value;
    }
}
