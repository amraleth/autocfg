package dev.amraleth.autocfg;

import dev.amraleth.autocfg.annotation.DefaultEntry;
import dev.amraleth.autocfg.annotation.Default;
import dev.amraleth.autocfg.annotation.DefaultValue;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.ArrayList;

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

    @Test
    void resolvesTypedDefaultsForPrimitivesBoxesAndTextConverters() {
        ConfigConverters.register(UUID.class, node -> UUID.fromString(node.toString()), UUID::toString);

        TypedDefaults config = ConfigMapper.read(new YamlConfiguration(), TypedDefaults.class);

        assertEquals(true, config.primitiveBoolean());
        assertEquals(Boolean.FALSE, config.boxedBoolean());
        assertEquals((byte) 1, config.primitiveByte());
        assertEquals(Byte.valueOf((byte) 2), config.boxedByte());
        assertEquals((short) 3, config.primitiveShort());
        assertEquals(Short.valueOf((short) 4), config.boxedShort());
        assertEquals(5, config.primitiveInteger());
        assertEquals(Integer.valueOf(6), config.boxedInteger());
        assertEquals(7L, config.primitiveLong());
        assertEquals(Long.valueOf(8L), config.boxedLong());
        assertEquals(1.5F, config.primitiveFloat());
        assertEquals(Float.valueOf(2.5F), config.boxedFloat());
        assertEquals(3.5D, config.primitiveDouble());
        assertEquals(Double.valueOf(4.5D), config.boxedDouble());
        assertEquals('z', config.primitiveCharacter());
        assertEquals(Character.valueOf('q'), config.boxedCharacter());
        assertEquals(List.of(9, 10), config.integers());
        assertEquals(Mode.FAST, config.mode());
        assertEquals(Duration.ofSeconds(30), config.timeout());
        assertEquals(NamespacedKey.minecraft("stone"), config.key());
        assertEquals(Material.STONE, config.material());
        assertEquals(UUID.fromString("3c3c2c9d-196d-43ac-96d8-0f381bf7983e"), config.id());
        assertEquals(List.of(), config.emptyBackups());
    }

    @Test
    void rejectsATypeMismatchedDefault() {
        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> ConfigMapper.read(new YamlConfiguration(), MismatchedDefault.class));

        assertEquals("@Default.String on number does not match int", exception.getMessage());
    }

    @Test
    void supportsDeprecatedTextDefaults() {
        LegacyDefaults config = ConfigMapper.read(new YamlConfiguration(), LegacyDefaults.class);

        assertEquals(7, config.count());
        assertEquals(List.of("one", "two"), config.labels());
    }

    @Test
    void rejectsDefaultsOnOptionalsAndRecords() {
        assertThrows(IllegalStateException.class,
                () -> ConfigMapper.read(new YamlConfiguration(), DefaultedOptional.class));
        assertThrows(IllegalStateException.class,
                () -> ConfigMapper.read(new YamlConfiguration(), DefaultedRecord.class));
    }

    @Test
    void appliesMigrationsAndUnknownKeyPolicies(@TempDir @NonNull Path directory) throws IOException {
        Path file = directory.resolve("config.yml");
        YamlConfiguration document = new YamlConfiguration();
        document.set("marker", "x");
        document.set("labels", List.of("one"));
        document.set("database.host", "localhost");
        document.set("database.port", 5432);
        document.set("backups", List.of());
        document.set("future", true);
        document.save(file.toFile());
        List<String> unknown = new ArrayList<>();
        ConfigLoadOptions options = new ConfigLoadOptions(UnknownKeyPolicy.WARN,
                List.of(config -> config.set("schema-version", 2)), unknown::add);

        ConfigLoader.load(file.toFile(), ExampleConfig.class, options);

        YamlConfiguration written = YamlConfiguration.loadConfiguration(file.toFile());
        assertEquals(2, written.getInt("schema-version"));
        assertEquals(List.of("future", "schema-version"), unknown.stream().sorted().toList());

        ConfigLoader.load(file.toFile(), ExampleConfig.class, ConfigLoadOptions.removeUnknownKeys());
        YamlConfiguration pruned = YamlConfiguration.loadConfiguration(file.toFile());
        assertFalse(pruned.contains("future"));
        assertFalse(pruned.contains("schema-version"));
    }

    @Test
    void neverOverwritesMalformedYaml(@TempDir @NonNull Path directory) throws IOException {
        Path file = directory.resolve("config.yml");
        String invalidYaml = "broken: [";
        Files.writeString(file, invalidYaml);

        assertThrows(IOException.class, () -> ConfigLoader.load(file.toFile(), ExampleConfig.class));

        assertEquals(invalidYaml, Files.readString(file));
    }

    record ExampleConfig(
            @Default.Character('x') char marker,
            @Default.String({"one", "two"}) List<String> labels,
            DatabaseConfig database,
            @DefaultEntry List<BackupConfig> backups,
            Optional<String> nickname
    ) {
    }

    record DatabaseConfig(
            @Default.String("localhost") String host,
            @Default.Integer(5432) int port
    ) {
    }

    record BackupConfig(
            @Default.String("backup") String name,
            @Default.Duration("PT1H") Duration interval
    ) {
    }

    record TypedDefaults(
            @Default.Boolean(true) boolean primitiveBoolean,
            @Default.Boolean(false) Boolean boxedBoolean,
            @Default.Byte(1) byte primitiveByte,
            @Default.Byte(2) Byte boxedByte,
            @Default.Short(3) short primitiveShort,
            @Default.Short(4) Short boxedShort,
            @Default.Integer(5) int primitiveInteger,
            @Default.Integer(6) Integer boxedInteger,
            @Default.Long(7) long primitiveLong,
            @Default.Long(8) Long boxedLong,
            @Default.Float(1.5F) float primitiveFloat,
            @Default.Float(2.5F) Float boxedFloat,
            @Default.Double(3.5D) double primitiveDouble,
            @Default.Double(4.5D) Double boxedDouble,
            @Default.Character('z') char primitiveCharacter,
            @Default.Character('q') Character boxedCharacter,
            @Default.Integer({9, 10}) List<Integer> integers,
            @Default.Enum("fast") Mode mode,
            @Default.Duration("PT30S") Duration timeout,
            @Default.NamespacedKey("minecraft:stone") NamespacedKey key,
            @Default.Material("stone") Material material,
            @Default.Text("3c3c2c9d-196d-43ac-96d8-0f381bf7983e") UUID id,
            @Default.Empty List<BackupConfig> emptyBackups
    ) {
    }

    enum Mode {
        FAST,
        SLOW
    }

    record MismatchedDefault(@Default.String("one") int number) {
    }

    record LegacyDefaults(@DefaultValue("7") int count,
                          @DefaultValue({"one", "two"}) List<String> labels) {
    }

    record DefaultedOptional(@Default.String("value") Optional<String> value) {
    }

    record DefaultedRecord(@Default.String("ignored") DatabaseConfig database) {
    }
}
