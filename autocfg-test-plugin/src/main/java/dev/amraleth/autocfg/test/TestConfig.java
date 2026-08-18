package dev.amraleth.autocfg.test;

import dev.amraleth.autocfg.CommonValidators;
import dev.amraleth.autocfg.annotation.ConfigComment;
import dev.amraleth.autocfg.annotation.ConfigKey;
import dev.amraleth.autocfg.annotation.DefaultEntry;
import dev.amraleth.autocfg.annotation.Default;
import org.bukkit.Material;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

record TestConfig(
        @ConfigComment("The display name.")
        @Default.String("name")
        String name,

        @ConfigComment("The material to place.")
        @Default.Material("stone")
        Material material,

        @ConfigComment("The materials to accept.")
        @Default.Material({"stone", "dirt"})
        List<Material> materials,

        @ConfigComment("An alternate display name.")
        Optional<String> nickname,

        @ConfigComment("How long to wait, as an ISO-8601 duration.")
        @Default.Duration("PT30S")
        Duration timeout,

        @ConfigComment("How often to retry a failed HTTP request.")
        @Default.Integer(3)
        int maxHTTPRetries,

        @ConfigComment("Database configuration.")
        @ConfigKey("database")
        DatabaseConfig database,

        @ConfigComment("The scheduled backups.")
        @DefaultEntry
        List<BackupConfig> backups
) {

    record DatabaseConfig(
            @ConfigComment("The host of the database.")
            @Default.String("localhost")
            String host,

            @ConfigComment("The port of the database.")
            @Default.Integer(4242)
            int port
    ) {

        public DatabaseConfig {
            CommonValidators.notBlank(host, "host")
                            .portRange(port);
        }
    }

    record BackupConfig(
            @Default.String("backup")
            String label,

            @Default.Duration("PT1H")
            Duration interval
    ) {
    }
}
