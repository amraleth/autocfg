package dev.amraleth.autocfg.annotation;

import org.jspecify.annotations.NonNull;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Represents a configuration field that has a different name than the Java field's name.
 *
 * <pre>
 * {@code
 * @ConfigComment("Some name")
 * @ConfigKey("key")
 * @DefaultValue("Name")
 * String name
 * }
 * </pre>
 * will turn into
 * <pre>
 * {@code
 * # Some name
 * key: Name <- the applied key "key"
 * }
 * </pre>
 *
 * @author amraleth
 * @since 1.0.0
 */

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.RECORD_COMPONENT})
public @interface ConfigKey {

    /**
     * The key to use in the configuration file instead of the component's name.
     *
     * @return The configuration key. Must not be blank or contain {@code '.'}.
     */
    @NonNull String value();

}
