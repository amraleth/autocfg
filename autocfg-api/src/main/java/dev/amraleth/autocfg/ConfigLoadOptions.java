package dev.amraleth.autocfg;

import org.bukkit.configuration.file.YamlConfiguration;

import java.util.List;
import java.util.function.Consumer;

/**
 * Immutable options that control migrations and handling of unknown root keys while loading.
 *
 * @param unknownKeyPolicy   The policy for root keys absent from the configuration record.
 * @param migrations         Ordered hooks that mutate the parsed document before it is mapped.
 * @param unknownKeyListener Listener invoked for each unknown root key when policy is {@code WARN}.
 * @author amraleth
 * @since 1.2.0
 */
public record ConfigLoadOptions(UnknownKeyPolicy unknownKeyPolicy,
                                List<Consumer<YamlConfiguration>> migrations,
                                Consumer<String> unknownKeyListener) {

    public ConfigLoadOptions {
        migrations = List.copyOf(migrations);
    }

    /**
     * Returns options that preserve unknown root keys and apply no migrations.
     *
     * @return Immutable default options.
     */
    public static ConfigLoadOptions defaults() {
        return new ConfigLoadOptions(UnknownKeyPolicy.PRESERVE, List.of(), ignored -> {
        });
    }

    /**
     * Returns options that remove unknown root keys and apply no migrations.
     *
     * @return Immutable removal options.
     */
    public static ConfigLoadOptions removeUnknownKeys() {
        return new ConfigLoadOptions(UnknownKeyPolicy.REMOVE, List.of(), ignored -> {
        });
    }

    /**
     * Returns options that report unknown root keys to the supplied listener.
     *
     * @param listener The listener to receive each unknown root key.
     * @return Immutable warning options.
     */
    public static ConfigLoadOptions warnUnknownKeys(Consumer<String> listener) {
        return new ConfigLoadOptions(UnknownKeyPolicy.WARN, List.of(), listener);
    }
}
