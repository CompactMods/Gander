@file:Suppress("SpellCheckingInspection")

import org.ajoberstar.grgit.Grgit
import java.text.SimpleDateFormat
import java.util.*

var envVersion: String = System.getenv("VERSION") ?: "9.9.9"
if (envVersion.startsWith("v"))
    envVersion = envVersion.trimStart('v')

val modId: String = prop("mod_id")
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
    alias(neoforged.plugins.userdev)
    id("org.ajoberstar.grgit") version ("5.2.1")
}

base {
    archivesName.set(modId)
    group = prop("mod_group_id")
    version = envVersion
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
    // toolchain.vendor.set(JvmVendorSpec.JETBRAINS)
}

evaluationDependsOn(":rendering")

minecraft {
    modIdentifier.set(modId)
    accessTransformers {
        file(project.project(":rendering").file("src/main/resources/META-INF/accesstransformer.cfg"))
        file(project.file("src/main/resources/META-INF/accesstransformer.cfg"))
        expose(project.file("src/main/resources/META-INF/accesstransformer.cfg"))
    }
}

runs {
    configureEach {
        systemProperty("forge.logging.markers", "") // 'SCAN,REGISTRIES,REGISTRYDUMP'
        systemProperty("forge.logging.console.level", "debug")
        if (!System.getenv().containsKey("CI")) {
            // JetBrains Runtime Hotswap
            // jvmArgument("-XX:+AllowEnhancedClassRedefinition")
        }

        modSource(sourceSets.main.get())
        modSource(project(":levels").sourceSets.main.get())
        modSource(project(":rendering").sourceSets.main.get())
    }

    create("client") {
        programArguments("--username", "Nano")
        programArguments("--width", "1920")
        programArguments("--height", "1080")
    }

    create("clientAuthed") {
        this.configure("client")
        this.workingDirectory(file("runs/client"))
        programArguments("--width", "1920")
        programArguments("--height", "1080")

        // known issue with DevLogin support on kotlin
        // need to use the FQN
        /*devLogin {
            enabled = true
        }*/

        configure<net.neoforged.gradle.dsl.common.runs.run.RunDevLogin> {
            enabled(true)
        }
    }

    create("remoteClient") {
        this.configure("client")
        this.workingDirectory(file("runs/client"))

        programArguments("--username", "Nano")
        programArguments("--width", "1920")
        programArguments("--height", "1080")

        jvmArgument("-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005")
    }

    create("server")
}

repositories {
    mavenLocal()
    // FIXME: Remove once PR publishing becomes available
    maven("https://maven.apexstudios.dev/private") {
        name = "Apex's Maven"
        content {
            includeGroup("net.neoforged")
        }
    }

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
}

dependencies {
    // Core Projects and Libraries
    implementation(neoforged.neoforge)

    implementation(project(":levels", "default"))
    implementation(project(":rendering", "default"))

    // Mods
    //mod(mods.bundles.jei)
    //mod(mods.jade)
}

tasks.withType<ProcessResources> {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
    options.compilerArgs.addAll(arrayOf("-Xmaxerrs", "1000"))
}

tasks.withType<Jar> {
    val mainGit = Grgit.open {
        currentDir = project.rootDir
    }

    manifest {
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
            "Main-Commit" to mainGit.head().id
        )
    }
}

tasks.withType<ProcessResources>().configureEach {
    filesMatching("META-INF/*.toml") {
        expand(
            "minecraft_version" to mojang.versions.minecraft.get(),
            "neo_version" to neoforged.versions.neoforge.get(),
            "minecraft_version_range" to prop("minecraft_version_range"),
            "neo_version_range" to prop("neo_version_range"),
            "loader_version_range" to prop("loader_version_range"),
            "mod_id" to modId,
            "mod_name" to prop("mod_name"),
            "mod_license" to prop("mod_license"),
            "mod_version" to envVersion,
            "mod_authors" to prop("mod_authors"),
            "mod_description" to prop("mod_description")
        )
    }
}

val PACKAGES_URL = System.getenv("GH_PKG_URL") ?: "https://maven.pkg.github.com/compactmods/gander"
publishing {
    publications.register<MavenPublication>(modId) {
        artifactId = "$modId-neoforge"
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
