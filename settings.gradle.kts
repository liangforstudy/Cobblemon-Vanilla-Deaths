pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
        maven("https://maven.fabricmc.net/")
        maven("https://maven.kikugie.dev/releases") { name = "KikuGie Releases" }
        maven("https://maven.kikugie.dev/snapshots") { name = "KikuGie Snapshots" }
    }
}

plugins {
    id("dev.kikugie.stonecutter") version "0.9.8"
    id("dev.kikugie.loom-back-compat") version "0.4.2"
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

// One Fabric build per Minecraft version. Each node pins its own Cobblemon in
// stonecutter.properties.toml; findings per Cobblemon version go in NOTES.md.
stonecutter {
    create(rootProject) {
        versions("1.21.1")
        vcsVersion = "1.21.1"
    }
}

rootProject.name = "cobblemon_vanilla_death"
