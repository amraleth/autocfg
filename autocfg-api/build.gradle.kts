plugins {
    id("java-library")
    id("maven-publish")
}

dependencies {
    compileOnly(libs.paper.api)
    compileOnly(libs.jspecify)
}

java {
    withSourcesJar()
    withJavadocJar()
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
        }
    }
}
