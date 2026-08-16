plugins {
    alias(libs.plugins.run.paper) apply false
    alias(libs.plugins.plugin.yml) apply false
}

val javaVersion = libs.versions.java.get()

allprojects {
    group = "dev.amraleth"
    version = "1.0.0-RELEASE"
}

subprojects {
    apply(plugin = "java")

    repositories {
        mavenCentral()

        maven {
            name = "papermc"
            url = uri("https://repo.papermc.io/repository/maven-public/")
        }
    }

    extensions.configure<JavaPluginExtension> {
        toolchain.languageVersion.set(JavaLanguageVersion.of(javaVersion))
    }
}
