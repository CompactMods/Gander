dependencyResolutionManagement {
    addVersionCatalog("mojang")
    addVersionCatalog("neoforged")
//    addVersionCatalog("mods")

//    versionCatalogs.create("mods") {
//        this.library("jei-common", "mezz.jei", "jei-1.20.4-common-api").versionRef("jei")
//        this.library("jei-neo", "mezz.jei", "jei-1.20.4-neoforge-api").versionRef("jei");
//        this.bundle("jei", listOf("jei-common", "jei-neo"))
//        this.version("jei", "17.3.0.49")
//
//        this.library("jade", "curse.maven", "jade-324717").version("5109393")
//    }
}

pluginManagement {
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

include("core")
include("levels")
include("rendering")
include("ui")

include("testmod")

fun DependencyResolutionManagement.addVersionCatalog(name: String) {
    this.versionCatalogs.create(name) {
        from(files("./gradle/$name.versions.toml"))
    }
}
