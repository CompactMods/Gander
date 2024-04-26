@file:Suppress("SpellCheckingInspection")

import org.ajoberstar.grgit.Grgit

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
    id("net.neoforged.gradle.userdev") version ("7.0.107")
    id("org.ajoberstar.grgit") version ("5.2.1")
}

base {
    archivesName.set("levels")
    group = "dev.compactmods.gander"
    version = envVersion
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}

jarJar.enable()

runs {}

repositories {
    mavenLocal()
}

dependencies {
    compileOnly(libraries.neoforge)
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
        // val now = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ").format(Date())
        // val name = prop("mod_name")

        val attrs = mapOf<String, Any>(
            /*"Specification-Title" to name,
            "Specification-Vendor" to "CompactMods",
            "Specification-Version" to "1",
            "Implementation-Title" to name,
            "Implementation-Version" to envVersion,
            "Implementation-Vendor" to "CompactMods",
            "Implementation-Timestamp" to now,
            "Minecraft-Version" to libraries.versions.minecraft.get(),
            "NeoForge-Version" to libraries.versions.neoforge.get(),
            "Main-Commit" to mainGit.head().id,*/
            "FMLModType" to "GAMELIBRARY",
            "Automatic-Module-Name" to "ganderlevels"
        )

        attributes(attrs)
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
    publications.register<MavenPublication>("levels") {
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
