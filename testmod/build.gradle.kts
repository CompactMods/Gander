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
    id("net.neoforged.gradle.userdev") version ("7.0.107")
    id("org.ajoberstar.grgit") version ("5.2.1")
}

base {
    archivesName.set(modId)
    group = prop("mod_group_id")
    version = envVersion
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}

jarJar.enable()

minecraft {
    modIdentifier.set(modId)
    accessTransformers {
        file(project.file("src/main/resources/META-INF/accesstransformer.cfg"))
    }
}

runs {
    configureEach {
        systemProperty("forge.logging.markers", "") // 'SCAN,REGISTRIES,REGISTRYDUMP'
        systemProperty("forge.logging.console.level", "debug")
        /*if (!System.getenv().containsKey("CI")) {
            // JetBrains Runtime Hotswap
            jvmArgument("-XX:+AllowEnhancedClassRedefinition")
        }*/

        modSource(sourceSets.main.get())
        modSource(project(":levels").sourceSets.main.get())
        modSource(project(":rendering").sourceSets.main.get())
        dependencies {
//             runtime(project(":levels"))
//             runtime(project(":rendering"))
        }
    }

    create("client") {
        programArguments("--username", "Nano")
        programArguments("--width", "1920")
        programArguments("--height", "1080")
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
}

dependencies {
    // Core Projects and Libraries
    implementation(libraries.neoforge)
    implementation(project(":levels", "default"))
    implementation(project(":rendering", "default"))

    // Mods
    // compileOnly(mods.bundles.jei)
    // compileOnly(mods.jade)
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
            "Minecraft-Version" to libraries.versions.minecraft.get(),
            "NeoForge-Version" to libraries.versions.neoforge.get(),
            "Main-Commit" to mainGit.head().id
        )
    }
}

tasks.withType<ProcessResources>().configureEach {
    filesMatching("META-INF/neoforge.mods.toml") {
        expand(
            "minecraft_version" to libraries.versions.minecraft.get(),
            "neo_version" to libraries.versions.neoforge.get(),
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

tasks.jar {
    archiveClassifier.set("slim")
    from(sourceSets.main.get().output)
}

tasks.jarJar {
    archiveClassifier.set("")
    from(sourceSets.main.get().output)
}

val PACKAGES_URL = System.getenv("GH_PKG_URL") ?: "https://maven.pkg.github.com/compactmods/gander"
publishing {
    publications.register<MavenPublication>(modId) {
        artifactId = "$modId-neoforge"
        // this.artifact(tasks.jarJar)
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
