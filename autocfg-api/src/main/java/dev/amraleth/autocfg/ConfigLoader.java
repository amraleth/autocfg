package dev.amraleth.autocfg;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
import org.jspecify.annotations.NonNull;

import java.io.File;
import java.io.IOException;

/**
 * Entrypoint for loading a configuration.
 *
 * @author amraleth
 * @since 1.0.0
 */
public final class ConfigLoader {

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
    public static <T extends Record> @NonNull T loadDefaultConfig(@NonNull Plugin plugin, @NonNull Class<T> type) throws IOException {
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
    public static <T extends Record> @NonNull T load(@NonNull Plugin plugin, @NonNull Class<T> type) throws IOException {
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
    public static <T extends Record> @NonNull T load(@NonNull Plugin plugin, @NonNull String name, @NonNull Class<T> type) throws IOException {
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
    public static <T extends Record> @NonNull T load(@NonNull File file, @NonNull Class<T> type) throws IOException {
        if (file.isDirectory()) {
            throw new IOException("%s is a directory, expected a configuration file".formatted(file));
        }
        File parent = file.getParentFile();
        if (parent != null && !parent.isDirectory() && !parent.mkdirs()) {
            throw new IOException("Cannot create directory %s".formatted(parent));
        }
        YamlConfiguration document = read(file);
        T loaded = ConfigMapper.read(document, type);
        ConfigWriter.write(document, loaded);
        document.save(file);
        return loaded;
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
    private static @NonNull YamlConfiguration read(@NonNull File file) throws IOException {
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
    private static @NonNull String fileName(@NonNull Class<?> type) {
        return Components.kebab(type.getSimpleName()) + ".yml";
    }

}
