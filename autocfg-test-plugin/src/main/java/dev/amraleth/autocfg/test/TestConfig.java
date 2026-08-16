package dev.amraleth.autocfg.test;

import dev.amraleth.autocfg.CommonValidators;
import dev.amraleth.autocfg.annotation.ConfigComment;
import dev.amraleth.autocfg.annotation.ConfigKey;
import dev.amraleth.autocfg.annotation.DefaultValue;
import org.bukkit.Material;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

record TestConfig(
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
        @ConfigKey("database")
        DatabaseConfig database,

        @ConfigComment("The scheduled backups.")
        @DefaultValue({})
        List<BackupConfig> backups
) {

    record DatabaseConfig(
            @ConfigComment("The host of the database.")
            @DefaultValue("localhost")
            String host,

            @ConfigComment("The port of the database.")
            @DefaultValue("4242")
            int port
    ) {

        public DatabaseConfig {
            CommonValidators.notBlank(host, "host");
            CommonValidators.portRange(port);
        }
    }

    record BackupConfig(
            @DefaultValue("backup")
            String label,

            @DefaultValue("PT1H")
            Duration interval
    ) {
    }
}
