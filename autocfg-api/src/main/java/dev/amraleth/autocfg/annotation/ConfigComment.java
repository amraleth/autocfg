package dev.amraleth.autocfg.annotation;

import org.jspecify.annotations.NonNull;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Represents a configuration comment. A comment is applied to a field inside the actual yml configuration.
 *
 * <pre>
 * {@code
 * @ConfigComment("Some name")
 * @Default.String("Name")
 * String name
 * }
 * </pre>
 * will translate to
 * <pre>
 * {@code
 * # Some name <- the applied comment
 * name: Name
 * }
 * </pre>
 *
 * <p>On a record type, the comment applies to every component of that type carrying no comment of
 * its own:
 * <pre>
 * {@code
 * @ConfigComment("Database configuration")
 * record DatabaseConfig(...) { }
 * }
 * </pre>
 *
 * @author amraleth
 * @since 1.0.0
 */

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.RECORD_COMPONENT, ElementType.TYPE})
public @interface ConfigComment {

    /**
     * The comment lines, one per rendered {@code #} line.
     *
     * @return The comment lines.
     */
    @NonNull String[] value();
}
