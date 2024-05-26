import org.ajoberstar.grgit.Grgit
import java.text.SimpleDateFormat
import java.util.*

plugins {
    id("idea")
    id("java-library")
    id("maven-publish")
    alias(neoforged.plugins.moddev)
    alias(libraries.plugins.grgit)
    alias(libraries.plugins.idea.ext)
}

val MOD_VERSION = System.getenv("VERSION") ?: "9.9.9"
val IS_RELEASE = System.getenv("RELEASE").toBoolean()
val IS_CI = System.getenv("CI_BUILD").toBoolean()
val JAVA_VERSION = JavaLanguageVersion.of(libraries.versions.java.get())
val GIT = Grgit.open { currentDir = project.rootDir }
val PACKAGES_URL = System.getenv("GH_PKG_URL") ?: "https://maven.pkg.github.com/compactmods/gander"

base {
    archivesName = "rendering"
    group = "dev.compactmods.gander"
    version = MOD_VERSION
}

idea.module {
    if(!IS_CI) {
        isDownloadSources = true
        isDownloadJavadoc = true
    }

    excludeDirs.addAll(files(
        ".gradle",
        ".idea",
        ".build",
        "gradle"
    ))
}

java {
    withSourcesJar()
    withJavadocJar()

    toolchain {
        vendor = if(IS_CI) JvmVendorSpec.ADOPTIUM else JvmVendorSpec.JETBRAINS
        languageVersion.set(JAVA_VERSION)
    }
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
    options.release.set(JAVA_VERSION.asInt())
    options.compilerArgs.addAll(arrayOf("-Xmaxerrs", "9000"))

    javaToolchains.compilerFor { languageVersion.set(JAVA_VERSION) }
}

tasks.withType<ProcessResources> {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

tasks.jar {
    manifest {
        from("src/main/resources/META-INF/MANIFEST.MF")

        attributes(mapOf(
            "Specification-Title" to "levels",
            "Specification-Vendor" to "CompactMods",
            "Specification-Version" to "1",
            "Implementation-Title" to "levels",
            "Implementation-Version" to MOD_VERSION,
            "Implementation-Vendor" to "CompactMods",
            "Implementation-Timestamp" to SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ").format(Date()),
            "Minecraft-Version" to mojang.versions.minecraft.get(),
            "NeoForge-Version" to neoforged.versions.neoforge.get(),
            "Main-Commit" to GIT.head().id
        ))
    }
}

repositories {
    // mavenLocal()
    maven("https://prmaven.neoforged.net/NeoForge/pr959")
}

neoForge {
    version = neoforged.versions.neoforge

    if(!IS_CI) {
        parchment {
            mappingsVersion = neoforged.versions.parchment.mappings
            minecraftVersion = neoforged.versions.parchment.minecraft
        }
    }

    accessTransformers.add("src/main/resources/META-INF/accesstransformer.cfg")
    // accessTransformers.expose("src/main/resources/META-INF/accesstransformer.cfg")
}

publishing {
    publications.register<MavenPublication>("rendering") {
        from(components.getByName("java"))
    }

    repositories {
        maven(PACKAGES_URL) {
            name = "GitHubPackages"

            credentials {
                username = System.getenv("GITHUB_ACTOR")
                password = System.getenv("GITHUB_TOKEN")
            }
        }
    }
}
