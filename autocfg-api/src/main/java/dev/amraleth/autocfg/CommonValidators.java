package dev.amraleth.autocfg;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Pattern;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Validators for use in the compact constructor of a configuration record.
 *
 * <p>An exception thrown from a compact constructor propagates out of the loader unchanged, so
 * these read naturally at the point of declaration:
 *
 * <pre>
 * {@code
 * record DatabaseConfig(String host, int port) {
 *     DatabaseConfig {
 *         CommonValidators.notBlank(host, "host");
 *         CommonValidators.portRange(port);
 *     }
 * }
 * }
 * </pre>
 *
 * @author amraleth
 * @since 1.0.0
 */
public final class CommonValidators {

    private static final Chain CHAIN = new Chain();

    /**
     * The lowest valid port number.
     */
    private static final int MIN_PORT = 1;

    /**
     * The highest valid port number.
     */
    private static final int MAX_PORT = 65535;

    private CommonValidators() {
    }

    /**
     * Checks that an {@code int} value lies within an inclusive range.
     *
     * @param value The value to check.
     * @param lower The lower bound, inclusive.
     * @param upper The upper bound, inclusive.
     * @return The fluent validation chain.
     * @throws IllegalArgumentException If the value lies outside the range.
     */
    public static @NonNull Chain intRange(int value, int lower, int upper) {
        if (!(lower <= value && value <= upper)) {
            throw new IllegalArgumentException("Value must be between %s and %s inclusive, is %s"
                    .formatted(lower, upper, value));
        }
        return CHAIN;
    }

    /**
     * Checks that an {@code int} value lies strictly within a range.
     *
     * @param value The value to check.
     * @param lower The lower bound, exclusive.
     * @param upper The upper bound, exclusive.
     * @return The fluent validation chain.
     * @throws IllegalArgumentException If the value lies outside the range.
     */
    public static @NonNull Chain intRangeExclusive(int value, int lower, int upper) {
        if (!(lower < value && value < upper)) {
            throw new IllegalArgumentException("Value must be between %s and %s exclusive, is %s"
                    .formatted(lower, upper, value));
        }
        return CHAIN;
    }

    /**
     * Checks that a {@code long} value lies within an inclusive range.
     *
     * @param value The value to check.
     * @param lower The lower bound, inclusive.
     * @param upper The upper bound, inclusive.
     * @return The fluent validation chain.
     * @throws IllegalArgumentException If the value lies outside the range.
     */
    public static @NonNull Chain longRange(long value, long lower, long upper) {
        if (!(lower <= value && value <= upper)) {
            throw new IllegalArgumentException("Value must be between %s and %s inclusive, is %s"
                    .formatted(lower, upper, value));
        }
        return CHAIN;
    }

    /**
     * Checks that a {@code long} value lies strictly within a range.
     *
     * @param value The value to check.
     * @param lower The lower bound, exclusive.
     * @param upper The upper bound, exclusive.
     * @return The fluent validation chain.
     * @throws IllegalArgumentException If the value lies outside the range.
     */
    public static @NonNull Chain longRangeExclusive(long value, long lower, long upper) {
        if (!(lower < value && value < upper)) {
            throw new IllegalArgumentException("Value must be between %s and %s exclusive, is %s"
                    .formatted(lower, upper, value));
        }
        return CHAIN;
    }

    /**
     * Checks that a {@code double} value lies within an inclusive range.
     *
     * @param value The value to check.
     * @param lower The lower bound, inclusive.
     * @param upper The upper bound, inclusive.
     * @return The fluent validation chain.
     * @throws IllegalArgumentException If the value lies outside the range.
     */
    public static @NonNull Chain doubleRange(double value, double lower, double upper) {
        if (!(lower <= value && value <= upper)) {
            throw new IllegalArgumentException("Value must be between %s and %s inclusive, is %s"
                    .formatted(lower, upper, value));
        }
        return CHAIN;
    }

    /**
     * Checks that a {@code double} value lies strictly within a range.
     *
     * @param value The value to check.
     * @param lower The lower bound, exclusive.
     * @param upper The upper bound, exclusive.
     * @return The fluent validation chain.
     * @throws IllegalArgumentException If the value lies outside the range.
     */
    public static @NonNull Chain doubleRangeExclusive(double value, double lower, double upper) {
        if (!(lower < value && value < upper)) {
            throw new IllegalArgumentException("Value must be between %s and %s exclusive, is %s"
                    .formatted(lower, upper, value));
        }
        return CHAIN;
    }

    /**
     * Checks that a {@code double} is neither {@code NaN} nor infinite.
     *
     * @param value The value to check.
     * @param name  The name to report in the failure message.
     * @return The fluent validation chain.
     * @throws IllegalArgumentException If the value is not finite.
     */
    public static @NonNull Chain finite(double value, @NonNull String name) {
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException("%s must be finite, is %s".formatted(name, value));
        }
        return CHAIN;
    }

    /**
     * Checks that a value is a valid port number.
     *
     * @param value The value to check.
     * @return The fluent validation chain.
     * @throws IllegalArgumentException If the value is not a valid port number.
     */
    public static @NonNull Chain portRange(int value) {
        return intRange(value, MIN_PORT, MAX_PORT);
    }

    /**
     * Checks that a value is positive.
     *
     * @param value The value to check.
     * @param name  The name to report in the failure message.
     * @return The fluent validation chain.
     * @throws IllegalArgumentException If the value is zero or negative.
     */
    public static @NonNull Chain positive(int value, @NonNull String name) {
        if (value <= 0) {
            throw new IllegalArgumentException("%s must be positive, is %s".formatted(name, value));
        }
        return CHAIN;
    }

    /**
     * Checks that a {@code long} value is positive.
     *
     * @param value The value to check.
     * @param name  The name to report in the failure message.
     * @return The fluent validation chain.
     * @throws IllegalArgumentException If the value is zero or negative.
     */
    public static @NonNull Chain positive(long value, @NonNull String name) {
        if (value <= 0) {
            throw new IllegalArgumentException("%s must be positive, is %s".formatted(name, value));
        }
        return CHAIN;
    }

    /**
     * Checks that a {@code double} value is positive.
     *
     * @param value The value to check.
     * @param name  The name to report in the failure message.
     * @return The fluent validation chain.
     * @throws IllegalArgumentException If the value is zero, negative, or {@code NaN}.
     */
    public static @NonNull Chain positive(double value, @NonNull String name) {
        if (!(value > 0)) {
            throw new IllegalArgumentException("%s must be positive, is %s".formatted(name, value));
        }
        return CHAIN;
    }

    /**
     * Checks that an {@code int} value is zero or positive.
     *
     * @param value The value to check.
     * @param name  The name to report in the failure message.
     * @return The fluent validation chain.
     * @throws IllegalArgumentException If the value is negative.
     */
    public static @NonNull Chain nonNegative(int value, @NonNull String name) {
        if (value < 0) {
            throw new IllegalArgumentException("%s must not be negative, is %s".formatted(name, value));
        }
        return CHAIN;
    }

    /**
     * Checks that a {@code long} value is zero or positive.
     *
     * @param value The value to check.
     * @param name  The name to report in the failure message.
     * @return The fluent validation chain.
     * @throws IllegalArgumentException If the value is negative.
     */
    public static @NonNull Chain nonNegative(long value, @NonNull String name) {
        if (value < 0) {
            throw new IllegalArgumentException("%s must not be negative, is %s".formatted(name, value));
        }
        return CHAIN;
    }

    /**
     * Checks that a {@code double} value is zero or positive.
     *
     * @param value The value to check.
     * @param name  The name to report in the failure message.
     * @return The fluent validation chain.
     * @throws IllegalArgumentException If the value is negative or {@code NaN}.
     */
    public static @NonNull Chain nonNegative(double value, @NonNull String name) {
        if (!(value >= 0)) {
            throw new IllegalArgumentException("%s must not be negative, is %s".formatted(name, value));
        }
        return CHAIN;
    }

    /**
     * Checks that an object reference is not {@code null}.
     *
     * @param value The value to check.
     * @param name  The name to report in the failure message.
     * @return The fluent validation chain.
     * @throws IllegalArgumentException If the value is {@code null}.
     */
    public static @NonNull Chain notNull(@Nullable Object value, @NonNull String name) {
        if (value == null) {
            throw new IllegalArgumentException("%s must not be null".formatted(name));
        }
        return CHAIN;
    }

    /**
     * Checks that a value is neither empty nor whitespace only.
     *
     * @param value The value to check.
     * @param name  The name to report in the failure message.
     * @return The fluent validation chain.
     * @throws IllegalArgumentException If the value is blank.
     */
    public static @NonNull Chain notBlank(@NonNull String value, @NonNull String name) {
        if (value.isBlank()) {
            throw new IllegalArgumentException("%s must not be blank".formatted(name));
        }
        return CHAIN;
    }

    /**
     * Checks that a {@link CharSequence} is not empty. Whitespace is accepted; use
     * {@link #notBlank(String, String)} when it is not.
     *
     * @param value The text to check.
     * @param name  The name to report in the failure message.
     * @return The fluent validation chain.
     * @throws IllegalArgumentException If the text is empty.
     */
    public static @NonNull Chain notEmpty(@NonNull CharSequence value, @NonNull String name) {
        if (value.isEmpty()) {
            throw new IllegalArgumentException("%s must not be empty".formatted(name));
        }
        return CHAIN;
    }

    /**
     * Checks that a {@link CharSequence} has an inclusive length range.
     *
     * @param value The text to check.
     * @param lower The minimum length, inclusive.
     * @param upper The maximum length, inclusive.
     * @param name  The name to report in the failure message.
     * @return The fluent validation chain.
     * @throws IllegalArgumentException If the text length lies outside the range.
     */
    public static @NonNull Chain lengthRange(@NonNull CharSequence value, int lower, int upper,
                                             @NonNull String name) {
        if (!(lower <= value.length() && value.length() <= upper)) {
            throw new IllegalArgumentException("%s length must be between %s and %s inclusive, is %s"
                    .formatted(name, lower, upper, value.length()));
        }
        return CHAIN;
    }

    /**
     * Checks that a {@link CharSequence} fully matches a {@link Pattern}.
     *
     * @param value   The text to check.
     * @param pattern The regular expression to match.
     * @param name    The name to report in the failure message.
     * @return The fluent validation chain.
     * @throws IllegalArgumentException If the text does not fully match the pattern.
     */
    public static @NonNull Chain matches(@NonNull CharSequence value, @NonNull Pattern pattern,
                                         @NonNull String name) {
        if (!pattern.matcher(value).matches()) {
            throw new IllegalArgumentException("%s must match %s, is %s".formatted(name, pattern, value));
        }
        return CHAIN;
    }

    /**
     * Checks that a collection is not empty.
     *
     * @param value The collection to check.
     * @param name  The name to report in the failure message.
     * @return The fluent validation chain.
     * @throws IllegalArgumentException If the collection is empty.
     */
    public static @NonNull Chain notEmpty(@NonNull Collection<?> value, @NonNull String name) {
        if (value.isEmpty()) {
            throw new IllegalArgumentException("%s must not be empty".formatted(name));
        }
        return CHAIN;
    }

    /**
     * Checks that a {@link Collection} has an inclusive size range.
     *
     * @param value The collection to check.
     * @param lower The minimum size, inclusive.
     * @param upper The maximum size, inclusive.
     * @param name  The name to report in the failure message.
     * @return The fluent validation chain.
     * @throws IllegalArgumentException If the collection size lies outside the range.
     */
    public static @NonNull Chain sizeRange(@NonNull Collection<?> value, int lower, int upper,
                                           @NonNull String name) {
        if (!(lower <= value.size() && value.size() <= upper)) {
            throw new IllegalArgumentException("%s size must be between %s and %s inclusive, is %s"
                    .formatted(name, lower, upper, value.size()));
        }
        return CHAIN;
    }

    /**
     * Checks that a {@link Collection} contains no duplicate values according to {@link Object#equals}.
     *
     * @param values The collection to check.
     * @return The fluent validation chain.
     * @throws IllegalArgumentException If duplicate values are present.
     */
    public static @NonNull Chain requireUnique(@NonNull Collection<?> values) {
        if (new HashSet<>(values).size() != values.size()) {
            throw new IllegalArgumentException("Duplicate value in collection");
        }
        return CHAIN;
    }

    /**
     * Checks if a given collection has unique sub-elements.
     *
     * @param list The collection to check.
     * @param key  The key to check for.
     * @param <T>  The Type of entries in the collection.
     * @param <K>  The type of the sub-element to check.
     * @return The fluent validation chain.
     * @throws IllegalArgumentException If two entries produce equal keys.
     */
    public static <T, K> @NonNull Chain requireUniqueBy(@NonNull Collection<T> list,
                                                        @NonNull Function<? super T, ? extends K> key) {
        Set<K> seen = new HashSet<>();
        list.forEach(item -> {
            K value = key.apply(item);
            if (!seen.add(value)) {
                throw new IllegalArgumentException("Duplicate value: " + value);
            }
        });
        return CHAIN;
    }

    /**
     * Stateless continuation returned by every validator. Each method validates its own argument
     * and returns this continuation, allowing independent configuration values to be checked in a
     * single fluent statement. Every instance method has the same parameter, type-parameter,
     * return, and exception contract as the identically named static method in
     * {@link CommonValidators}; it delegates directly to that method.
     */
    public static final class Chain {

        private Chain() {
        }

        public @NonNull Chain intRange(int value, int lower, int upper) {
            return CommonValidators.intRange(value, lower, upper);
        }

        public @NonNull Chain intRangeExclusive(int value, int lower, int upper) {
            return CommonValidators.intRangeExclusive(value, lower, upper);
        }

        public @NonNull Chain longRange(long value, long lower, long upper) {
            return CommonValidators.longRange(value, lower, upper);
        }

        public @NonNull Chain longRangeExclusive(long value, long lower, long upper) {
            return CommonValidators.longRangeExclusive(value, lower, upper);
        }

        public @NonNull Chain doubleRange(double value, double lower, double upper) {
            return CommonValidators.doubleRange(value, lower, upper);
        }

        public @NonNull Chain doubleRangeExclusive(double value, double lower, double upper) {
            return CommonValidators.doubleRangeExclusive(value, lower, upper);
        }

        public @NonNull Chain finite(double value, @NonNull String name) {
            return CommonValidators.finite(value, name);
        }

        public @NonNull Chain portRange(int value) {
            return CommonValidators.portRange(value);
        }

        public @NonNull Chain positive(int value, @NonNull String name) {
            return CommonValidators.positive(value, name);
        }

        public @NonNull Chain positive(long value, @NonNull String name) {
            return CommonValidators.positive(value, name);
        }

        public @NonNull Chain positive(double value, @NonNull String name) {
            return CommonValidators.positive(value, name);
        }

        public @NonNull Chain nonNegative(int value, @NonNull String name) {
            return CommonValidators.nonNegative(value, name);
        }

        public @NonNull Chain nonNegative(long value, @NonNull String name) {
            return CommonValidators.nonNegative(value, name);
        }

        public @NonNull Chain nonNegative(double value, @NonNull String name) {
            return CommonValidators.nonNegative(value, name);
        }

        public @NonNull Chain notNull(@Nullable Object value, @NonNull String name) {
            return CommonValidators.notNull(value, name);
        }

        public @NonNull Chain notBlank(@NonNull String value, @NonNull String name) {
            return CommonValidators.notBlank(value, name);
        }

        public @NonNull Chain nonBlank(@NonNull String value, @NonNull String name) {
            return notBlank(value, name);
        }

        public @NonNull Chain notEmpty(@NonNull CharSequence value, @NonNull String name) {
            return CommonValidators.notEmpty(value, name);
        }

        public @NonNull Chain notEmpty(@NonNull Collection<?> value, @NonNull String name) {
            return CommonValidators.notEmpty(value, name);
        }

        public @NonNull Chain lengthRange(@NonNull CharSequence value, int lower, int upper, @NonNull String name) {
            return CommonValidators.lengthRange(value, lower, upper, name);
        }

        public @NonNull Chain matches(@NonNull CharSequence value, @NonNull Pattern pattern, @NonNull String name) {
            return CommonValidators.matches(value, pattern, name);
        }

        public @NonNull Chain sizeRange(@NonNull Collection<?> value, int lower, int upper, @NonNull String name) {
            return CommonValidators.sizeRange(value, lower, upper, name);
        }

        public @NonNull Chain requireUnique(@NonNull Collection<?> values) {
            return CommonValidators.requireUnique(values);
        }

        public <T, K> @NonNull Chain requireUniqueBy(@NonNull Collection<T> list, @NonNull Function<? super T, ? extends K> key) {
            return CommonValidators.requireUniqueBy(list, key);
        }
    }
}
