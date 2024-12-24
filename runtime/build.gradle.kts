@file:Suppress("SpellCheckingInspection")

import org.ajoberstar.grgit.Grgit
import java.text.SimpleDateFormat
import java.util.*

plugins {
    id("idea")
    id("eclipse")
    id("maven-publish")
    id("java-library")
    alias(neoforged.plugins.userdev)
    id("org.ajoberstar.grgit") version ("5.2.1")
}

java.toolchain.languageVersion = JavaLanguageVersion.of(21)
base.archivesName = "${project.name}-neoforge"

minecraft {
    modIdentifier = "gander"
    accessTransformers {
        file(project.file("src/main/resources/META-INF/accesstransformer.cfg"))
        expose(project.file("src/main/resources/META-INF/accesstransformer.cfg"))

        consume(project(":rendering"))
    }
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

dependencies {
    compileOnly(neoforged.neoforge)

    api(project(":rendering"))
    api(project(":levels"))
}

runs {
    configureEach {
        systemProperty("log4j2.configurationFile", file("../log4j2.xml").absolutePath)

        modSource(sourceSets["main"])
        modSource(project(":levels").sourceSets["main"])
        modSource(project(":rendering").sourceSets["main"])
    }

    register("client") {
        arguments("--width", "1920")
        arguments("--height", "1080")
    }

    register("server")
}

tasks.withType<Jar> {
    val mainGit = Grgit.open {
        currentDir = project.rootDir
    }

    manifest {
        val now = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ").format(Date())

        attributes(
            "Specification-Title" to project.name,
            "Specification-Vendor" to providers.gradleProperty("vendor"),
            "Specification-Version" to project.version,
            "Implementation-Title" to project.name,
            "Implementation-Version" to project.version,
            "Implementation-Vendor" to providers.gradleProperty("vendor"),
            "Implementation-Timestamp" to now,
            "Minecraft-Version" to mojang.versions.minecraft,
            "NeoForge-Version" to neoforged.versions.neoforge,
            "Main-Commit" to mainGit.head().id
        )
    }
}

tasks.withType<ProcessResources>().configureEach {
    val replacements = mapOf(
        "mod_id" to minecraft.modIdentifier,
        "mod_version" to project.version,

        "loader_version" to neoforged.versions.loader,
        "neo_version" to neoforged.versions.neoforge,
        "minecraft_version" to mojang.versions.minecraft
    )

    inputs.properties(replacements)

    filesMatching("META-INF/*.toml") {
        expand(replacements.mapValues { if (it.value is Provider<*>) (it.value as Provider<*>).get() else it.value })
    }
}

publishing {
    publications.register<MavenPublication>(project.name) {
        from(components["java"])
    }

    repositories {
        maven(providers.gradleProperty("publishing.github.url")) {
            name = "publishing.github"
        }
    }
}
