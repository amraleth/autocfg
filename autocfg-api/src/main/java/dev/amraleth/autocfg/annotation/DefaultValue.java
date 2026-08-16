package dev.amraleth.autocfg.annotation;

import org.jspecify.annotations.NonNull;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Represents the default value for a field. Required for every component that is not a record, a
 * record list or an {@link java.util.Optional}. A component absent from the file with no default
 * declared raises a runtime exception.
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
 * key: Name <- the default value "Name" applied
 * }
 * </pre>
 *
 * @author amraleth
 * @since 1.0.0
 */

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.RECORD_COMPONENT})
public @interface DefaultValue {

    /**
     * The default value as text: a single literal for a scalar component, one literal per entry for
     * a list component.
     *
     * @return The default literals.
     */
    @NonNull String[] value();

}
