plugins {
    id("java-library")
    id("signing")
    alias(libs.plugins.maven.publish)
}

dependencies {
    compileOnly(libs.paper.api)
    compileOnly(libs.jspecify)
}

mavenPublishing {
    publishToMavenCentral()
    signAllPublications()

    coordinates(project.group as String, "autocfg", project.version as String)

    signing {
        useGpgCmd()
    }

    pom {
        name = "AutoCfg"
        description = "Record-based, annotation-driven configuration loading for Paper plugins."
        inceptionYear = "2026"
        url = "https://github.com/amraleth/autocfg"

        licenses {
            license {
                name = "The Apache License, Version 2.0"
                url = "https://www.apache.org/licenses/LICENSE-2.0.txt"
                distribution = "https://www.apache.org/licenses/LICENSE-2.0.txt"
            }
        }

        developers {
            developer {
                id = "amraleth"
                name = "Patrick Vollandt"
                url = "https://github.com/amraleth"
            }
        }

        scm {
            url = "https://github.com/amraleth/autocfg"
            connection = "scm:git:git://github.com/amraleth/autocfg.git"
            developerConnection = "scm:git:ssh://git@github.com/amraleth/autocfg.git"
        }
    }
}
