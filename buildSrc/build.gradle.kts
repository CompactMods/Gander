plugins {
    `kotlin-dsl`
    id("org.jetbrains.gradle.plugin.idea-ext") version "1.1.8"
}

val IS_CI = System.getenv("CI_BUILD").toBoolean()

idea.module {
    if(!IS_CI) {
        isDownloadSources = true
        isDownloadJavadoc = true
    }

    excludeDirs.addAll(files(
        ".gradle",
        "build"
    ))
}

repositories {
    gradlePluginPortal()
    mavenCentral()

    maven("https://maven.neoforged.net/releases") {
        content {
            includeGroup("net.neoforged")
        }
    }

    maven("https://prmaven.neoforged.net/ModDevGradle/pr118") {
        content {
            includeModule("net.neoforged.moddev.legacyforge", "net.neoforged.moddev.legacyforge.gradle.plugin")
            includeModule("net.neoforged.moddev.repositories", "net.neoforged.moddev.repositories.gradle.plugin")
            includeModule("net.neoforged", "moddev-gradle")
            includeModule("net.neoforged.moddev", "net.neoforged.moddev.gradle.plugin")
        }
    }
}

dependencies {
    // working against MDG PR build (118)
    // https://github.com/neoforged/ModDevGradle/pull/118
    implementation("net.neoforged.moddev:net.neoforged.moddev.gradle.plugin:2.0.60-beta-pr-118-legacy")
}
