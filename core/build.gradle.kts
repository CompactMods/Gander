@file:Suppress("SpellCheckingInspection")

import java.text.SimpleDateFormat
import java.util.*

var envVersion: String = System.getenv("VERSION") ?: "9.9.9"
if (envVersion.startsWith("v"))
    envVersion = envVersion.trimStart('v')

plugins {
    id("maven-publish")
    id("java-library")
    alias(neoforged.plugins.moddev)
}

base {
    archivesName.set("core")
    group = "dev.compactmods.gander"
    version = envVersion
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}

neoForge {
    version = neoforged.versions.neoforge

    parchment {
        minecraftVersion = libs.versions.parchmentMC
        mappingsVersion = libs.versions.parchment
    }
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
    options.compilerArgs.addAll(arrayOf("-Xmaxerrs", "1000"))
}

tasks.withType<Jar> {
    val gitVersion = providers.exec {
        commandLine("git", "rev-parse", "HEAD")
    }.standardOutput.asText.get().trimEnd()

    manifest {
        val now = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ").format(Date())

        val attrs = mapOf<String, Any>(
            "Automatic-Module-Name" to "gandercore",
            "Specification-Title" to name,
            "Specification-Vendor" to "CompactMods",
            "Specification-Version" to "1",
            "Implementation-Title" to name,
            "Implementation-Version" to envVersion,
            "Implementation-Vendor" to "CompactMods",
            "Implementation-Timestamp" to now,
            "Minecraft-Version" to mojang.versions.minecraft.get(),
            "NeoForge-Version" to neoforged.versions.neoforge.get(),
            "Main-Commit" to gitVersion,
            "FMLModType" to "GAMELIBRARY",
        )

        attributes(attrs)
    }
}

val PACKAGES_URL = System.getenv("GH_PKG_URL") ?: "https://maven.pkg.github.com/compactmods/gander"
publishing {
    publications.register<MavenPublication>("core") {
        from(components.getByName("java"))
    }

    repositories {
        // GitHub Packages
        maven(PACKAGES_URL) {
            name = "GitHubPackages"
            credentials {
                username = System.getenv("GITHUB_ACTOR")
                password = System.getenv("GITHUB_TOKEN")
            }
        }
    }
}
