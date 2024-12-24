@file:Suppress("SpellCheckingInspection")

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
    alias(neoforged.plugins.moddev)
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
evaluationDependsOn(":ui")

var atProjects = listOf(project(":rendering"))

val renderNurseCfg by configurations.creating

neoForge {
    version = neoforged.versions.neoforge

    this.mods.create(modId) {
        modSourceSets.add(sourceSets.main)
    }

    accessTransformers {
        atProjects.forEach {
            val f = it.file("src/main/resources/META-INF/accesstransformer.cfg")
            if(f.exists())
                from(f)
        }

        from(project.file("src/main/resources/META-INF/accesstransformer.cfg"))
        publish(project.file("src/main/resources/META-INF/accesstransformer.cfg"))
    }

    runs {
        configureEach {
            systemProperty("forge.logging.markers", "") // 'SCAN,REGISTRIES,REGISTRYDUMP'
            systemProperty("forge.logging.console.level", "debug")
            if (!System.getenv().containsKey("CI")) {
                // JetBrains Runtime Hotswap
                // jvmArgument("-XX:+AllowEnhancedClassRedefinition")
            }
        }

        create("client") {
            client()
            gameDirectory.set(file("runs/client"))

            programArguments.addAll("--username", "Nano")
            programArguments.addAll("--width", "1920")
            programArguments.addAll("--height", "1080")
        }

        create("clientAuthed") {
            client()

            gameDirectory.set(file("runs/client"))

            programArguments.addAll("--width", "1920")
            programArguments.addAll("--height", "1080")

            // known issue with DevLogin support on kotlin
            // need to use the FQN
            /*devLogin {
            enabled = true
        }*/

//        configure<net.neoforged.gradle.dsl.common.runs.run.RunDevLogin> {
//            enabled(true)
//        }
        }

        if(System.getenv("RENDERDOC_LIB") != null) {
            create("renderDoc") {
                client()
                gameDirectory.set(file("runs/client"))

                programArguments.addAll("--username", "Nano")
                programArguments.addAll("--width", "1920")
                programArguments.addAll("--height", "1080")

                systemProperty("neoforge.rendernurse.renderdoc.library", System.getenv("RENDERDOC_LIB"))

                if(org.gradle.internal.os.OperatingSystem.current().isLinux) {
                    environment("LD_PRELOAD", System.getenv("RENDERDOC_LIB"))
                } else {
                    // parses out the render nurse jar path and adds as `-javaagent:${path}`
                    jvmArguments.addAll(renderNurseCfg.incoming.files.elements.map { it.map { "-javaagent:${it.asFile.absolutePath}" } })
                    jvmArguments.addAll(
                        "--enable-preview",
                        "--enable-native-access=ALL-UNNAMED"
                    )
                }
            }
        }

        create("server") {
            server()
            gameDirectory.set(file("runs/server"))
        }
    }
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
}

dependencies {
    // Core Projects and Libraries
    implementation(project(":levels"))
    implementation(project(":rendering"))
    implementation(project(":ui"))

    renderNurseCfg("net.neoforged:render-nurse:0.0.12")

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
    val gitVersion = providers.exec {
        commandLine("git", "rev-parse", "HEAD")
    }.standardOutput.asText.get()

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
            "Main-Commit" to gitVersion
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
