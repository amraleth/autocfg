package dev.amraleth.autocfg;

import dev.amraleth.autocfg.annotation.DefaultEntry;
import dev.amraleth.autocfg.annotation.DefaultValue;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ConfigLoaderTest {

    @Test
    void loadsDefaultsAndWritesThemToDisk(@TempDir @NonNull Path directory) throws IOException {
        Path file = directory.resolve("config.yml");

        ExampleConfig config = ConfigLoader.load(file.toFile(), ExampleConfig.class);

        assertEquals(new ExampleConfig('x', List.of("one", "two"), new DatabaseConfig("localhost", 5432),
                List.of(new BackupConfig("backup", Duration.ofHours(1))), Optional.empty()), config);

        YamlConfiguration written = YamlConfiguration.loadConfiguration(file.toFile());
        assertEquals("x", written.getString("marker"));
        assertEquals(List.of("one", "two"), written.getStringList("labels"));
        assertEquals("localhost", written.getString("database.host"));
        assertEquals(5432, written.getInt("database.port"));
        assertEquals("backup", written.getMapList("backups").getFirst().get("name"));
        assertFalse(written.contains("nickname"));
    }

    @Test
    void readsExistingValuesAndLeavesOptionalValuesAbsent(@TempDir @NonNull Path directory) throws IOException {
        Path file = directory.resolve("config.yml");
        YamlConfiguration document = new YamlConfiguration();
        document.set("marker", "q");
        document.set("labels", List.of("configured"));
        document.set("database.host", "db.internal");
        document.set("database.port", 3306);
        document.set("backups", List.of());
        document.save(file.toFile());

        ExampleConfig config = ConfigLoader.load(file.toFile(), ExampleConfig.class);

        assertEquals('q', config.marker());
        assertEquals(List.of("configured"), config.labels());
        assertEquals(new DatabaseConfig("db.internal", 3306), config.database());
        assertEquals(List.of(), config.backups());
        assertEquals(Optional.empty(), config.nickname());
    }

    @Test
    void rejectsInvalidCharacterValues(@TempDir @NonNull Path directory) throws IOException {
        Path file = directory.resolve("config.yml");
        YamlConfiguration document = new YamlConfiguration();
        document.set("marker", "too long");
        document.set("labels", List.of("configured"));
        document.set("database.host", "db.internal");
        document.set("database.port", 3306);
        document.set("backups", List.of());
        document.save(file.toFile());

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> ConfigLoader.load(file.toFile(), ExampleConfig.class));

        assertEquals("Cannot decode marker: Expected a single character, got too long", exception.getMessage());
    }

    record ExampleConfig(
            @DefaultValue("x") char marker,
            @DefaultValue({"one", "two"}) List<String> labels,
            DatabaseConfig database,
            @DefaultEntry List<BackupConfig> backups,
            Optional<String> nickname
    ) {
    }

    record DatabaseConfig(
            @DefaultValue("localhost") String host,
            @DefaultValue("5432") int port
    ) {
    }

    record BackupConfig(
            @DefaultValue("backup") String name,
            @DefaultValue("PT1H") Duration interval
    ) {
    }
}
