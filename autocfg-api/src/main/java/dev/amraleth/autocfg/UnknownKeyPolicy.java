package dev.amraleth.autocfg;

/**
 * Policy for root configuration keys that are not represented by the record type.
 *
 * <p>The policy is applied after migrations and successful record mapping. Nested unknown keys
 * are preserved by every policy.
 *
 * @author amraleth
 * @since 1.2.0
 */
public enum UnknownKeyPolicy {
    /**
     * Keeps unknown root keys unchanged.
     */
    PRESERVE,
    /**
     * Reports each unknown root key to the configured listener.
     */
    WARN,
    /**
     * Removes unknown root keys before saving the configuration.
     */
    REMOVE
}
