package dev.amraleth.autocfg.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Seeds a record list with a single entry built from the element record's defaults. The record list
 * analogue of {@link DefaultValue}, and only meaningful on a {@code List<SomeRecord>} component,
 * where it replaces the {@code @DefaultValue({})} that such a component otherwise requires.
 *
 * <pre>
 * {@code
 * @ConfigComment("The scheduled backups.")
 * @DefaultEntry
 * List<BackupConfig> backups
 *
 * record BackupConfig(
 *         @DefaultValue("backup")
 *         String label,
 *
 *         @DefaultValue("PT1H")
 *         Duration interval
 * ) { }
 * }
 * </pre>
 * will turn into
 * <pre>
 * {@code
 * # The scheduled backups.
 * backups:
 * - label: backup   <- the seeded entry
 *   interval: PT1H
 * }
 * </pre>
 *
 * <p>The entry is seeded only when the key is absent from the file. Once written it reads back as
 * ordinary data, so it can be edited, duplicated or removed; a list left as {@code []} stays empty.
 *
 * <p>Every component of the element record must be resolvable without a file, that is carry a
 * {@link DefaultValue}, be a record whose components are, or be an {@link java.util.Optional}.
 *
 * @author amraleth
 * @since 1.1.0
 */

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.RECORD_COMPONENT})
public @interface DefaultEntry {

}
