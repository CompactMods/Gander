@file:Suppress("SpellCheckingInspection")

import java.text.SimpleDateFormat
import java.util.*

var envVersion: String = System.getenv("VERSION") ?: "9.9.9"
if (envVersion.startsWith("v"))
    envVersion = envVersion.trimStart('v')

val isRelease: Boolean = (System.getenv("RELEASE") ?: "false").equals("true", true)

fun prop(name: String): String {
    if (project.properties.containsKey(name))
        return project.property(name) as String;

    return "";
}

plugins {
    id("idea")
    id("eclipse")
    id("maven-publish")
    id("java-library")
    alias(neoforged.plugins.moddev)
    id("org.ajoberstar.grgit") version ("5.2.1")
}

base {
    archivesName.set("rendering")
    group = "dev.compactmods.gander"
    version = envVersion
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}

neoForge {
    version = neoforged.versions.neoforge

    accessTransformers {
        file(project.file("src/main/resources/META-INF/accesstransformer.cfg"))
        publish(file("src/main/resources/META-INF/accesstransformer.cfg"))
    }

    parchment {
        minecraftVersion = libs.versions.parchmentMC
        mappingsVersion = libs.versions.parchment
    }
}

dependencies {
    implementation(project(":core"))
}

tasks.withType<ProcessResources> {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
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
        from("src/main/resources/META-INF/MANIFEST.MF")
        val now = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ").format(Date())
        val name = prop("mod_name")

        attributes(
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
            "Automatic-Module-Name" to "ganderlevels"
        )
    }
}

val PACKAGES_URL = System.getenv("GH_PKG_URL") ?: "https://maven.pkg.github.com/compactmods/gander"
publishing {
    publications.register<MavenPublication>("rendering") {
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
