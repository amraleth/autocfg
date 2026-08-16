package dev.amraleth.autocfg;

import java.util.Collection;

import org.jspecify.annotations.NonNull;

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
     * Checks that a value lies within an inclusive range.
     *
     * @param value The value to check.
     * @param lower The lower bound, inclusive.
     * @param upper The upper bound, inclusive.
     * @throws IllegalArgumentException If the value lies outside the range.
     */
    public static void intRange(int value, int lower, int upper) {
        if (!(lower <= value && value <= upper)) {
            throw new IllegalArgumentException("Value must be between %s and %s inclusive, is %s"
                    .formatted(lower, upper, value));
        }
    }

    /**
     * Checks that a value lies strictly within a range.
     *
     * @param value The value to check.
     * @param lower The lower bound, exclusive.
     * @param upper The upper bound, exclusive.
     * @throws IllegalArgumentException If the value lies outside the range.
     */
    public static void intRangeExclusive(int value, int lower, int upper) {
        if (!(lower < value && value < upper)) {
            throw new IllegalArgumentException("Value must be between %s and %s exclusive, is %s"
                    .formatted(lower, upper, value));
        }
    }

    /**
     * Checks that a value lies within an inclusive range.
     *
     * @param value The value to check.
     * @param lower The lower bound, inclusive.
     * @param upper The upper bound, inclusive.
     * @throws IllegalArgumentException If the value lies outside the range.
     */
    public static void doubleRange(double value, double lower, double upper) {
        if (!(lower <= value && value <= upper)) {
            throw new IllegalArgumentException("Value must be between %s and %s inclusive, is %s"
                    .formatted(lower, upper, value));
        }
    }

    /**
     * Checks that a value is a valid port number.
     *
     * @param value The value to check.
     * @throws IllegalArgumentException If the value is not a valid port number.
     */
    public static void portRange(int value) {
        intRange(value, MIN_PORT, MAX_PORT);
    }

    /**
     * Checks that a value is positive.
     *
     * @param value The value to check.
     * @param name  The name to report in the failure message.
     * @throws IllegalArgumentException If the value is zero or negative.
     */
    public static void positive(int value, @NonNull String name) {
        if (value <= 0) {
            throw new IllegalArgumentException("%s must be positive, is %s".formatted(name, value));
        }
    }

    /**
     * Checks that a value is neither empty nor whitespace only.
     *
     * @param value The value to check.
     * @param name  The name to report in the failure message.
     * @throws IllegalArgumentException If the value is blank.
     */
    public static void notBlank(@NonNull String value, @NonNull String name) {
        if (value.isBlank()) {
            throw new IllegalArgumentException("%s must not be blank".formatted(name));
        }
    }

    /**
     * Checks that a collection is not empty.
     *
     * @param value The collection to check.
     * @param name  The name to report in the failure message.
     * @throws IllegalArgumentException If the collection is empty.
     */
    public static void notEmpty(@NonNull Collection<?> value, @NonNull String name) {
        if (value.isEmpty()) {
            throw new IllegalArgumentException("%s must not be empty".formatted(name));
        }
    }
}
