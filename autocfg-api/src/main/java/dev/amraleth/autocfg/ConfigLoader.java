package dev.amraleth.autocfg;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

/**
 * Entrypoint for loading a configuration.
 *
 * @author amraleth
 * @since 1.0.0
 */
public final class ConfigLoader {

    /**
     * Prevents instantiation of this utility class.
     */
    private ConfigLoader() {
    }

    /**
     * Loads the default configuration, as in config.yml.
     *
     * @param plugin The plugin this configuration is a part of.
     * @param type   The type of config to load.
     * @param <T>    The type of config class to load.
     * @return An instance of the config class.
     * @throws IOException              If the file cannot be created, read, parsed or saved.
     * @throws IllegalStateException    If the config class is malformed or a required key is missing.
     * @throws IllegalArgumentException If a value in the file cannot be decoded or fails validation.
     */
    public static <T extends Record> T loadDefaultConfig(Plugin plugin, Class<T> type) throws IOException {
        return load(plugin, "config.yml", type);
    }

    /**
     * Loads a configuration whose name is inferred via {@link ConfigLoader#fileName(Class)} by the input config class.
     *
     * @param plugin The plugin this configuration is a part of.
     * @param type   The type of config to load.
     * @param <T>    The type of config class to load.
     * @return An instance of the config class.
     * @throws IOException              If the file cannot be created, read, parsed or saved.
     * @throws IllegalStateException    If the config class is malformed or a required key is missing.
     * @throws IllegalArgumentException If a value in the file cannot be decoded or fails validation.
     */
    public static <T extends Record> T load(Plugin plugin, Class<T> type) throws IOException {
        return load(plugin, fileName(type), type);
    }

    /**
     * Loads an arbitrary configuration in the plugin's data folder.
     *
     * @param plugin The plugin this configuration is a part of.
     * @param name   The name of the file.
     * @param type   The type of the config to load.
     * @param <T>    The type of config class to load.
     * @return An instance of the config class.
     * @throws IOException              If the file cannot be created, read, parsed or saved.
     * @throws IllegalStateException    If the config class is malformed or a required key is missing.
     * @throws IllegalArgumentException If a value in the file cannot be decoded or fails validation.
     */
    public static <T extends Record> T load(Plugin plugin, String name, Class<T> type) throws IOException {
        return load(new File(plugin.getDataFolder(), name), type);
    }

    /**
     * Loads an arbitrary configuration.
     *
     * @param file The file to load.
     * @param type The type of config to load.
     * @param <T>  The type of config class to load.
     * @return An instance of the config class.
     * @throws IOException              If the file cannot be created, read, parsed or saved.
     * @throws IllegalStateException    If the config class is malformed or a required key is missing.
     * @throws IllegalArgumentException If a value in the file cannot be decoded or fails validation.
     */
    public static <T extends Record> T load(File file, Class<T> type) throws IOException {
        return load(file, type, ConfigLoadOptions.defaults());
    }

    /**
     * Loads a configuration with schema-evolution options.
     *
     * @param file    The file to load.
     * @param type    The record type to load.
     * @param options Migration and unknown-key handling options.
     * @param <T>     The configuration record type.
     * @return The loaded configuration.
     * @throws IOException If the file cannot be created, read, parsed, or saved.
     */
    public static <T extends Record> T load(File file, Class<T> type,
                                            ConfigLoadOptions options) throws IOException {
        if (file.isDirectory()) {
            throw new IOException("%s is a directory, expected a configuration file".formatted(file));
        }
        File parent = file.getParentFile();
        if (parent != null && !parent.isDirectory() && !parent.mkdirs()) {
            throw new IOException("Cannot create directory %s".formatted(parent));
        }
        YamlConfiguration document = read(file);
        options.migrations().forEach(migration -> migration.accept(document));
        T loaded = ConfigMapper.read(document, type);
        ConfigWriter.write(document, loaded);
        handleUnknownKeys(document, type, options);
        document.save(file);
        return loaded;
    }

    /**
     * Applies the configured policy to unknown root keys after a successful mapping.
     *
     * @param document The configuration document to inspect.
     * @param type     The record type defining known root keys.
     * @param options  The configured unknown-key policy and listener.
     */
    private static void handleUnknownKeys(YamlConfiguration document, Class<? extends Record> type,
                                          ConfigLoadOptions options) {
        if (options.unknownKeyPolicy() == UnknownKeyPolicy.PRESERVE) {
            return;
        }
        Set<String> known = new HashSet<>();
        for (var component : type.getRecordComponents()) {
            known.add(Components.path("", component));
        }
        for (String key : Set.copyOf(document.getKeys(false))) {
            if (known.contains(key)) {
                continue;
            }
            if (options.unknownKeyPolicy() == UnknownKeyPolicy.WARN) {
                options.unknownKeyListener().accept(key);
            } else {
                document.set(key, null);
            }
        }
    }

    /**
     * Reads a document from disk, or an empty document if the file does not exist yet.
     *
     * <p>A malformed file raises an {@link IOException}; {@link #load(File, Class)} therefore never
     * writes defaults over a file it could not parse.
     *
     * @param file The file to read.
     * @return The parsed document.
     * @throws IOException If the file cannot be read or parsed.
     */
    private static YamlConfiguration read(File file) throws IOException {
        YamlConfiguration document = new YamlConfiguration();
        if (!file.isFile()) {
            return document;
        }
        try {
            document.load(file);
        } catch (InvalidConfigurationException exception) {
            throw new IOException("Cannot parse %s: %s".formatted(file, exception.getMessage()), exception);
        }
        return document;
    }

    /**
     * Fetches the file name of a config class.
     *
     * @param type The type to fetch the name for.
     * @return The config name with .yml appended.
     */
    private static String fileName(Class<?> type) {
        return Components.kebab(type.getSimpleName()) + ".yml";
    }

}
