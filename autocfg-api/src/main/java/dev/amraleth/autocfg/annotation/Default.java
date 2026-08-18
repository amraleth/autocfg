package dev.amraleth.autocfg.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Typed defaults for configuration record components.
 *
 * <p>Every value annotation accepts an array so the same annotation works for a scalar and for a
 * {@code List} of that scalar. Use exactly one nested annotation per component. {@link Text} is
 * for types represented as text, such as registered custom converters; the remaining text-backed
 * built-ins have their own annotations.
 */
public @interface Default {

    /**
     * Default for a {@code boolean}, {@link java.lang.Boolean}, or corresponding {@code List}.
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.RECORD_COMPONENT)
    @interface Boolean {
        /**
         * @return One scalar default, or one value for each list entry.
         */
        boolean[] value();
    }

    /**
     * Default for a {@code byte}, {@link java.lang.Byte}, or corresponding {@code List}.
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.RECORD_COMPONENT)
    @interface Byte {
        /**
         * @return One scalar default, or one value for each list entry.
         */
        byte[] value();
    }

    /**
     * Default for a {@code short}, {@link java.lang.Short}, or corresponding {@code List}.
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.RECORD_COMPONENT)
    @interface Short {
        /**
         * @return One scalar default, or one value for each list entry.
         */
        short[] value();
    }

    /**
     * Default for an {@code int}, {@link java.lang.Integer}, or corresponding {@code List}.
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.RECORD_COMPONENT)
    @interface Integer {
        /**
         * @return One scalar default, or one value for each list entry.
         */
        int[] value();
    }

    /**
     * Default for a {@code long}, {@link java.lang.Long}, or corresponding {@code List}.
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.RECORD_COMPONENT)
    @interface Long {
        /**
         * @return One scalar default, or one value for each list entry.
         */
        long[] value();
    }

    /**
     * Default for a {@code float}, {@link java.lang.Float}, or corresponding {@code List}.
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.RECORD_COMPONENT)
    @interface Float {
        /**
         * @return One scalar default, or one value for each list entry.
         */
        float[] value();
    }

    /**
     * Default for a {@code double}, {@link java.lang.Double}, or corresponding {@code List}.
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.RECORD_COMPONENT)
    @interface Double {
        /**
         * @return One scalar default, or one value for each list entry.
         */
        double[] value();
    }

    /**
     * Default for a {@code char}, {@link java.lang.Character}, or corresponding {@code List}.
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.RECORD_COMPONENT)
    @interface Character {
        /**
         * @return One scalar default, or one value for each list entry.
         */
        char[] value();
    }

    /**
     * Default for a {@link java.lang.String} or corresponding {@code List}.
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.RECORD_COMPONENT)
    @interface String {
        /**
         * @return One scalar default, or one value for each list entry.
         */
        java.lang.String[] value();
    }

    /**
     * Defaults an enum by its case-insensitive constant name.
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.RECORD_COMPONENT)
    @interface Enum {
        /**
         * @return One scalar default, or one value for each list entry.
         */
        java.lang.String[] value();
    }

    /**
     * Default for a {@link java.time.Duration} in ISO-8601 form.
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.RECORD_COMPONENT)
    @interface Duration {
        /**
         * @return One scalar default, or one value for each list entry.
         */
        java.lang.String[] value();
    }

    /**
     * Default for a Bukkit {@link org.bukkit.NamespacedKey}.
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.RECORD_COMPONENT)
    @interface NamespacedKey {
        /**
         * @return One scalar default, or one value for each list entry.
         */
        java.lang.String[] value();
    }

    /**
     * Default for a Bukkit {@link org.bukkit.Material}.
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.RECORD_COMPONENT)
    @interface Material {
        /**
         * @return One scalar default, or one value for each list entry.
         */
        java.lang.String[] value();
    }

    /**
     * Defaults a type handled by a consumer-registered converter from its textual form.
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.RECORD_COMPONENT)
    @interface Text {
        /**
         * @return One scalar default, or one value for each list entry.
         */
        java.lang.String[] value();
    }

    /**
     * Declares an empty list of records. {@link DefaultEntry} instead seeds one default record.
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.RECORD_COMPONENT)
    @interface Empty {
    }
}
