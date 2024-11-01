dependencyResolutionManagement {
    versionCatalogs.create("mojang") {
        version("minecraft", "1.21.1")
        version("minecraftRange") {
            this.require("[1.21, 1.21.2)")
            this.prefer("1.21.1")
        }
    }

    versionCatalogs.create("neoforged") {
        version("mdg", "2.0.42-beta")
        version("neoforge", "21.1.73")

        library("neoforge", "net.neoforged", "neoforge")
            .versionRef("neoforge")

        plugin("moddev", "net.neoforged.moddev")
            .versionRef("mdg")
    }

    versionCatalogs.create("mods") {
        this.library("jei-common", "mezz.jei", "jei-1.20.4-common-api").versionRef("jei")
        this.library("jei-neo", "mezz.jei", "jei-1.20.4-neoforge-api").versionRef("jei");
        this.bundle("jei", listOf("jei-common", "jei-neo"))
        this.version("jei", "17.3.0.49")

        this.library("jade", "curse.maven", "jade-324717").version("5109393")
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
include("testmod")
