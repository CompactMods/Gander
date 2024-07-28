dependencyResolutionManagement {
    versionCatalogs.create("mojang") {
        version("minecraft", "[1.21,1.22)")
    }

    versionCatalogs.create("neoforged") {
        version("neogradle", "7.0.191-pr-232-fix-no-recompile")
        version("neoforge", "21.0.143")
        version("loader", "[4.0.21,)")

        library("neoforge", "net.neoforged", "neoforge")
            .versionRef("neoforge")

        plugin("userdev", "net.neoforged.gradle.userdev")
            .versionRef("neogradle")
    }

    versionCatalogs.create("utilities") {
        library("roaringbitmap", "org.roaringbitmap", "RoaringBitmap")
            .version("[1.0,1.1)")
    }

    versionCatalogs.create("mods") {
        this.library("jei-common", "mezz.jei", "jei-1.20.4-common-api").versionRef("jei")
        this.library("jei-neo", "mezz.jei", "jei-1.20.4-neoforge-api").versionRef("jei");
        this.bundle("jei", listOf("jei-common", "jei-neo"))
        this.version("jei", "17.3.0.49")

        this.library("jade", "curse.maven", "jade-324717").version("5109393")
    }

    versionCatalogs.create("libraries") {
        this.library("devlogin", "net.covers1624", "DevLogin").version("0.1.+")
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

        maven("https://prmaven.neoforged.net/NeoGradle/pr232") {
            name = "NeoGradle PR #232"
            content {
                includeModule("net.neoforged.gradle.common", "net.neoforged.gradle.common.gradle.plugin")
                includeModule("net.neoforged.gradle", "utils")
                includeModule("net.neoforged.gradle", "platform")
                includeModule("net.neoforged.gradle", "common")
                includeModule("net.neoforged.gradle.platform", "net.neoforged.gradle.platform.gradle.plugin")
                includeModule("net.neoforged.gradle", "test-utils")
                includeModule("net.neoforged.gradle", "dsl-common")
                includeModule("net.neoforged.gradle", "neoform")
                includeModule("net.neoforged.gradle.neoform", "net.neoforged.gradle.neoform.gradle.plugin")
                includeModule("net.neoforged.gradle", "dsl-mixin")
                includeModule("net.neoforged.gradle.vanilla", "net.neoforged.gradle.vanilla.gradle.plugin")
                includeModule("net.neoforged.gradle", "dsl-userdev")
                includeModule("net.neoforged.gradle", "mixin")
                includeModule("net.neoforged.gradle", "vanilla")
                includeModule("net.neoforged.gradle", "dsl-neoform")
                includeModule("net.neoforged.gradle", "dsl-vanilla")
                includeModule("net.neoforged.gradle", "dsl-platform")
                includeModule("net.neoforged.gradle.mixin", "net.neoforged.gradle.mixin.gradle.plugin")
                includeModule("net.neoforged.gradle.userdev", "net.neoforged.gradle.userdev.gradle.plugin")
                includeModule("net.neoforged.gradle", "userdev")
            }
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