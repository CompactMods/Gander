dependencyResolutionManagement {
    versionCatalogs.create("mojang") {
        version("minecraft", "[1.21.4,1.22)")
    }

    versionCatalogs.create("neoforged") {
        version("neogradle", "7.0.171")
        version("neoforge", "21.4.32-beta")
        version("loader", "[6.0.4,)")

        library("neoforge", "net.neoforged", "neoforge")
            .versionRef("neoforge")

        plugin("userdev", "net.neoforged.gradle.userdev")
            .versionRef("neogradle")
    }
}

pluginManagement {
    plugins {
        id("idea")
        id("eclipse")
        id("maven-publish")
        id("java-library")
    }

    repositories {
        mavenLocal()
        mavenCentral()
        gradlePluginPortal()

        // maven("https://maven.architectury.dev/")

        maven("https://maven.parchmentmc.org") {
            name = "ParchmentMC"
        }

        maven("https://maven.neoforged.net/releases") {
            name = "NeoForged"
        }

        maven("https://repo.spongepowered.org/repository/maven-public/") {
            name = "Sponge Snapshots"
        }
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version ("0.8.0")
}

rootProject.name = "Gander"

include("levels")
include("rendering")

include("runtime")

include("gander_test")
