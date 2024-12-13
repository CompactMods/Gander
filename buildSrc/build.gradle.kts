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
}

dependencies {
    implementation("net.neoforged.moddev:net.neoforged.moddev.gradle.plugin:2.0.59-beta")
}
