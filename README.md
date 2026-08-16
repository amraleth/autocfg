# AutoCfg

[![Maven Central](https://img.shields.io/maven-central/v/dev.amraleth/autocfg?style=flat-square&logo=apachemaven&color=blue&label=Maven%20Central)](https://central.sonatype.com/artifact/dev.amraleth/autocfg)
[![Javadoc](https://img.shields.io/badge/Javadoc-online-blue?style=flat-square&logo=readthedocs)](https://javadoc.io/doc/dev.amraleth/autocfg)
[![License](https://img.shields.io/badge/License-Apache%202.0-green?style=flat-square)](LICENSE)
[![Java](https://img.shields.io/badge/Java-25-orange?style=flat-square&logo=openjdk)](https://openjdk.org/)

Record-based, annotation-driven configuration for Paper plugins.

Declare your configuration as a Java record, annotate it with defaults and
comments, and load it in one call. AutoCfg reads the file, decodes every value
into the right type, validates it, and writes the file back with any missing
keys filled in and your comments applied - so `config.yml` stays in sync with
the record without you maintaining it by hand.

## Quick start

Declare the shape of your config:

```java
public record MyConfig(
        @ConfigComment("The display name.")
        @DefaultValue("name")
        String name,

        @ConfigComment("The material to place.")
        @DefaultValue("stone")
        Material material,

        @ConfigComment("The materials to accept.")
        @DefaultValue({"stone", "dirt"})
        List<Material> materials,

        @ConfigComment("An alternate display name.")
        Optional<String> nickname,

        @ConfigComment("How long to wait, as an ISO-8601 duration.")
        @DefaultValue("PT30S")
        Duration timeout,

        @ConfigComment("How often to retry a failed HTTP request.")
        @DefaultValue("3")
        int maxHTTPRetries,

        @ConfigComment("Database configuration.")
        DatabaseConfig database
) {

    public record DatabaseConfig(
            @ConfigComment("The host of the database.")
            @DefaultValue("localhost")
            String host,

            @ConfigComment("The port of the database.")
            @DefaultValue("4242")
            int port
    ) {
        DatabaseConfig {
            CommonValidators.notBlank(host, "host");
            CommonValidators.portRange(port);
        }
    }
}
```

Load it:

```java
public final class MyPlugin extends JavaPlugin {

    @Override
    public void onEnable() {
        try {
            MyConfig config = ConfigLoader.loadDefaultConfig(this, MyConfig.class);
            getLogger().info(config.database().host());
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot load config.yml", exception);
        }
    }
}
```

On first start the plugin writes this `config.yml`:

```yaml
# The display name.
name: name
# The material to place.
material: stone
# The materials to accept.
materials:
- stone
- dirt
# How long to wait, as an ISO-8601 duration.
timeout: PT30S
# How often to retry a failed HTTP request.
max-http-retries: 3
# Database configuration.
database:
  # The host of the database.
  host: localhost
  # The port of the database.
  port: 4242
```

Note that `nickname` is absent: an empty `Optional` is not written out.

## Installation

Replace `VERSION` with the version in the badge above.

### Gradle (Kotlin DSL)

```kotlin
repositories {
    mavenCentral()
}

dependencies {
    implementation("dev.amraleth:autocfg:VERSION")
}
```

### Version catalog

```toml
[versions]
autocfg = "VERSION"

[libraries]
autocfg = { module = "dev.amraleth:autocfg", version.ref = "autocfg" }
```

```kotlin
dependencies {
    implementation(libs.autocfg)
}
```

### Maven

```xml
<dependency>
    <groupId>dev.amraleth</groupId>
    <artifactId>autocfg</artifactId>
    <version>VERSION</version>
</dependency>
```

### Shipping it with your plugin

AutoCfg has no transitive dependencies - `paper-api` and `jspecify` are
`compileOnly` - but the classes still have to reach the server at runtime.
Two ways:

**Let Paper download it.** Add it to the `libraries` list of your
`plugin.yml` / `paper-plugin.yml` and the server pulls it from Maven Central
on startup:

```yaml
libraries:
  - dev.amraleth:autocfg:VERSION
```

With the [plugin-yml](https://github.com/eldoriarpg/plugin-yml) Gradle plugin,
use the `library` configuration and the list is generated for you:

```kotlin
dependencies {
    library("dev.amraleth:autocfg:VERSION")
}
```

**Or shade it** into your plugin jar with the
[Shadow](https://github.com/GradleUp/shadow) plugin. Relocating the package is
recommended so that two plugins bundling different versions cannot clash:

```kotlin
tasks.shadowJar {
    relocate("dev.amraleth.autocfg", "com.example.myplugin.libs.autocfg")
}
```

## Reference

### Loading

| Call | File |
| ---- | ---- |
| `ConfigLoader.loadDefaultConfig(plugin, type)` | `config.yml` in the plugin's data folder |
| `ConfigLoader.load(plugin, type)` | Named after the record, kebab-cased - `MyConfig` becomes `my-config.yml` |
| `ConfigLoader.load(plugin, "other.yml", type)` | A named file in the plugin's data folder |
| `ConfigLoader.load(file, type)` | An arbitrary file |

Every call reads the file, fills in whatever is missing, and saves it back.
Missing parent directories are created.

### Annotations

| Annotation | Applies to | Effect |
| ---------- | ---------- | ------ |
| `@DefaultValue` | Record component | The value used when the key is absent, written as text. One literal per list entry. |
| `@DefaultEntry` | `List<SomeRecord>` component | Seeds the list with one entry built from the element record's defaults when the key is absent. |
| `@ConfigComment` | Record component, record type | Comment lines rendered above the key. On a type, it applies to every component of that type that carries no comment of its own. |
| `@ConfigKey` | Record component | Overrides the generated key. Must not be blank or contain `.`. |

Keys are derived from component names in kebab-case, splitting acronyms at
their trailing boundary - `maxHTTPRetries` becomes `max-http-retries`.

`@DefaultValue` is required on every component that is not a record, a record
list or an `Optional`. A component with neither a value in the file nor a
default raises `IllegalStateException`.

### Supported types

- Primitives and their boxes, and `String`
- Enums - written lowercase, read case-insensitively
- Nested records, mapped to nested sections
- `List<T>` of any supported scalar, and `List<SomeRecord>` for repeated sections,
  optionally seeded with an example entry
- `Optional<T>` - an absent key yields `Optional.empty()` and needs no default
- `Duration` (ISO-8601), `NamespacedKey`, and `Material`, registered out of the box

### Record lists

A `List<SomeRecord>` declares one of two things. `@DefaultValue({})` starts the
list empty, leaving the shape of an entry undocumented in the file. `@DefaultEntry`
seeds a single entry built from the element record's own `@DefaultValue`s, so a
fresh file shows what an entry looks like:

```java
@ConfigComment("The scheduled backups.")
@DefaultEntry
List<BackupConfig> backups

record BackupConfig(
        @DefaultValue("backup")
        String label,

        @DefaultValue("PT1H")
        Duration interval
) { }
```

```yaml
# The scheduled backups.
backups:
- label: backup
  interval: PT1H
```

Every component of a seeded record must be resolvable without a file - a
`@DefaultValue`, a record whose components are, or an `Optional`.

The entry is seeded only when the key is absent. Once written it reads back as
ordinary data, so it can be edited, duplicated or removed, and a list left as
`[]` stays empty. Note that a bare `backups:` with no value parses as null,
which counts as absent and seeds again; write `backups: []` to keep it empty.

A non-empty `@DefaultValue` on a record list is rejected either way - use
`@DefaultEntry` or declare the entries in the file.

Bukkit attaches comments to keys, and entries inside a list have none, so
`@ConfigComment` on the components of a list element record does not render. The
comment on the list component itself does.

### Custom types

Register a converter for anything else, before the config that uses it loads:

```java
ConfigConverters.register(UUID.class,
        node -> UUID.fromString(node.toString()),
        UUID::toString);
```

The registry is static, and therefore scoped to the classloader holding the
class: a relocated copy shaded into your plugin has a registry of its own, while
a copy loaded through `libraries` is shared across every plugin using it.

### Validation

Validate in the record's compact constructor. Exceptions thrown there propagate
out of the loader unchanged, so a bad value fails the load with your own
message. `CommonValidators` covers the usual cases: `intRange`,
`intRangeExclusive`, `doubleRange`, `portRange`, `positive`, `notBlank` and
`notEmpty`.

### Failures

| Exception | Cause |
| --------- | ----- |
| `IOException` | The file cannot be created, read, parsed or saved |
| `IllegalStateException` | The record is malformed, or a required key has no value and no default |
| `IllegalArgumentException` | A value cannot be decoded into its component type, or fails validation |

A file that cannot be parsed raises `IOException` before anything is written, so
a syntax error in `config.yml` never causes it to be overwritten with defaults.

## Requirements

- Java 25 or newer
- Paper 26.2 or newer

## Building

```bash
./gradlew build
```

`./gradlew :autocfg-test-plugin:runServer` starts a Paper server with the test
plugin installed.

## License

Licensed under the [Apache License 2.0](LICENSE).
