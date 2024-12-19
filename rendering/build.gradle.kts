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
base.archivesName = "rendering"

minecraft {
    accessTransformers {
        file(project.file("src/main/resources/META-INF/accesstransformer.cfg"))
        expose(project.file("src/main/resources/META-INF/accesstransformer.cfg"))
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
}

tasks.withType<Jar> {
    val mainGit = Grgit.open {
        currentDir = project.rootDir
    }

    manifest {
        from("src/main/resources/META-INF/MANIFEST.MF")
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
