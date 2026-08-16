import net.minecrell.pluginyml.bukkit.BukkitPluginDescription

plugins {
    alias(libs.plugins.run.paper)
    alias(libs.plugins.plugin.yml)
}

dependencies {
    implementation(project(":autocfg-api"))
    compileOnly(libs.paper.api)
    compileOnly(libs.jspecify)
}

tasks {
    jar {
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
        dependsOn(configurations.runtimeClasspath)
        from(configurations.runtimeClasspath.map { classpath ->
            classpath.filter { it.name.startsWith("autocfg") }.map { zipTree(it) }
        })
    }

    runServer {
        minecraftVersion(libs.versions.minecraft.get())
    }
}

paper {
    name = "AutoCfgTestPlugin"
    version = project.version as String
    apiVersion = libs.versions.minecraft.get()
    load = BukkitPluginDescription.PluginLoadOrder.STARTUP
    authors = listOf("Amraleth")
    main = "dev.amraleth.autocfg.test.AutoCfgTestPlugin"
}
