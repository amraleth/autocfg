package dev.amraleth.autocfg;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CommonValidatorsTest {

    @Test
    void validatesNumericRangesAndSigns() {
        assertDoesNotThrow(() -> CommonValidators.longRange(10L, 1L, 10L));
        assertDoesNotThrow(() -> CommonValidators.longRangeExclusive(5L, 1L, 10L));
        assertDoesNotThrow(() -> CommonValidators.doubleRangeExclusive(0.5D, 0D, 1D));
        assertDoesNotThrow(() -> CommonValidators.finite(1.5D, "rate"));
        assertDoesNotThrow(() -> CommonValidators.positive(1L, "count"));
        assertDoesNotThrow(() -> CommonValidators.positive(0.1D, "rate"));
        assertDoesNotThrow(() -> CommonValidators.nonNegative(0, "retries"));

        assertThrows(IllegalArgumentException.class, () -> CommonValidators.longRange(11L, 1L, 10L));
        assertThrows(IllegalArgumentException.class, () -> CommonValidators.doubleRangeExclusive(0D, 0D, 1D));
        assertThrows(IllegalArgumentException.class, () -> CommonValidators.finite(Double.NaN, "rate"));
        assertThrows(IllegalArgumentException.class, () -> CommonValidators.positive(0L, "count"));
        assertThrows(IllegalArgumentException.class, () -> CommonValidators.nonNegative(-1D, "rate"));
    }

    @Test
    void validatesTextAndCollections() {
        assertDoesNotThrow(() -> CommonValidators.notEmpty("name", "name"));
        assertDoesNotThrow(() -> CommonValidators.lengthRange("name", 1, 8, "name"));
        assertDoesNotThrow(() -> CommonValidators.matches("abc-123", Pattern.compile("[a-z]+-[0-9]+"), "id"));
        assertDoesNotThrow(() -> CommonValidators.sizeRange(List.of("a", "b"), 1, 2, "values"));
        assertDoesNotThrow(() -> CommonValidators.requireUnique(List.of("a", "b")));

        assertThrows(IllegalArgumentException.class, () -> CommonValidators.notEmpty("", "name"));
        assertThrows(IllegalArgumentException.class, () -> CommonValidators.lengthRange("name", 1, 3, "name"));
        assertThrows(IllegalArgumentException.class,
                () -> CommonValidators.matches("ABC", Pattern.compile("[a-z]+"), "id"));
        assertThrows(IllegalArgumentException.class, () -> CommonValidators.sizeRange(List.of(), 1, 2, "values"));
        assertThrows(IllegalArgumentException.class, () -> CommonValidators.requireUnique(List.of("a", "a")));
    }

    @Test
    void returnsNonNullValuesAndRejectsNulls() {
        assertDoesNotThrow(() -> CommonValidators.requireUnique(List.of("a", "b"))
                .nonBlank("value", "setting")
                .positive(1, "count"));
        assertThrows(IllegalArgumentException.class, () -> CommonValidators.notNull(null, "setting"));
    }
}
