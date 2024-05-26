dependencyResolutionManagement {
    versionCatalogs.create("mojang") {
        version("minecraft", "1.20.6")
    }

    versionCatalogs.create("neoforged") {
        version("neoforge", "20.6.94-beta-pr-959-features-gradle-metadata")
        version("parchment-mappings", "2024.05.01")
        version("parchment-minecraft", "1.20.6")
        plugin("moddev", "net.neoforged.moddev").version("0.1.36-pr-1-pr-publish")
    }

    versionCatalogs.create("mods") {
        /*version("jei", "17.3.0.49")
        library("jei-common", "mezz.jei", "jei-1.20.4-common-api").versionRef("jei")
        library("jei-neo", "mezz.jei", "jei-1.20.4-neoforge-api").versionRef("jei");
        bundle("jei", listOf("jei-common", "jei-neo"))

        library("jade", "curse.maven", "jade-324717").version("5109393")*/
    }

    versionCatalogs.create("libraries") {
        version("java", "21")
        plugin("grgit", "org.ajoberstar.grgit").version("5.2.1")
        plugin("idea-ext", "org.jetbrains.gradle.plugin.idea-ext").version("1.1.8")
        library("devlogin", "net.covers1624", "DevLogin").version("0.1.0.4")
    }
}

pluginManagement {
    repositories {
        gradlePluginPortal()
        maven("https://maven.neoforged.net/releases")
        maven("https://prmaven.neoforged.net/ModDevGradle/pr1")
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version ("0.8.0")
}

rootProject.name = "Gander"

include("levels")
include("rendering")
include("testmod")
