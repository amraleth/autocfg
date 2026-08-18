package dev.amraleth.autocfg.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Legacy text default.
 *
 * <p>Use the type-specific {@link Default} annotations for new configurations. This annotation is
 * retained for source and binary compatibility with configurations written before typed defaults.
 *
 * @author amraleth
 */
@Deprecated(since = "1.2.0")
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.RECORD_COMPONENT)
public @interface DefaultValue {

    /**
     * Returns one scalar literal or one literal for each list entry.
     *
     * @return The legacy text defaults.
     */
    String[] value();
}
