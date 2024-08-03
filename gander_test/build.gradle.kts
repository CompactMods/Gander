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
base.archivesName = "gander_test"

dependencies {
    // Core Projects and Libraries
    implementation(neoforged.neoforge)

    compileOnly(project(":runtime"))
}

//...graaaadle
evaluationDependsOn(":levels")
evaluationDependsOn(":rendering")
evaluationDependsOn(":runtime")

runs {
    configureEach {
        systemProperty("log4j2.configurationFile", file("../log4j2.xml").absolutePath)

        modSource(sourceSets["main"])
        modSources(project(":levels").sourceSets["main"])
        modSource(project(":rendering").sourceSets["main"])
        modSource(project(":runtime").sourceSets["main"])
    }

    create("client") {
        programArguments("--width", "1920")
        programArguments("--height", "1080")
    }

    create("clientRenderDoc") {
        configure("client")
        workingDirectory(runs["client"].workingDirectory)
        programArguments("--width", "1920")
        programArguments("--height", "1080")

        renderDoc.enabled = true
    }

    create("server")
}

repositories {
    mavenLocal()

    maven("https://maven.blamejared.com/") {
        // location of the maven that hosts JEI files since January 2023
        name = "Jared's maven"
    }

    maven("https://www.cursemaven.com") {
        content {
            includeGroup("curse.maven")
        }
    }

    maven("https://modmaven.dev") {
        // location of a maven mirror for JEI files, as a fallback
        name = "ModMaven"
    }

    maven("https://maven.covers1624.net") {
        // location for DevLogin
        name = "Covers's Maven"
    }
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
