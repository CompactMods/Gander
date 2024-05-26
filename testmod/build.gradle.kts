import org.ajoberstar.grgit.Grgit
import org.slf4j.event.Level
import java.text.SimpleDateFormat
import java.util.*

plugins {
    id("idea")
    id("java-library")
    alias(neoforged.plugins.moddev)
    alias(libraries.plugins.grgit)
    alias(libraries.plugins.idea.ext)
}

evaluationDependsOn(":levels")
evaluationDependsOn(":rendering")

val MOD_VERSION = System.getenv("VERSION") ?: "9.9.9"
val IS_RELEASE = System.getenv("RELEASE").toBoolean()
val IS_CI = System.getenv("CI_BUILD").toBoolean()
val JAVA_VERSION = JavaLanguageVersion.of(libraries.versions.java.get())
val GIT = Grgit.open { currentDir = project.rootDir }
val PACKAGES_URL = System.getenv("GH_PKG_URL") ?: "https://maven.pkg.github.com/compactmods/gander"

base {
    archivesName = "testmod"
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

java.toolchain {
    vendor = if(IS_CI) JvmVendorSpec.ADOPTIUM else JvmVendorSpec.JETBRAINS
    languageVersion.set(JAVA_VERSION)
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
    options.release.set(JAVA_VERSION.asInt())
    options.compilerArgs.addAll(arrayOf("-Xmaxerrs", "9000"))

    javaToolchains.compilerFor { languageVersion.set(JAVA_VERSION) }
}

tasks.withType<ProcessResources> {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE

    val replacements = mapOf(
        Pair("MOD_VERSION", MOD_VERSION)
    )

    inputs.properties(replacements)

    filesMatching("META-INF/neoforge.mods.toml") {
        expand(replacements)
    }
}

tasks.jar {
    manifest {
        attributes(mapOf(
            "Specification-Title" to "testmod",
            "Specification-Vendor" to "CompactMods",
            "Specification-Version" to "1",
            "Implementation-Title" to "testmod",
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

    accessTransformers.addAll(
        "src/main/resources/META-INF/accesstransformer.cfg",
        project(":rendering").file("src/main/resources/META-INF/accesstransformer.cfg").absolutePath
    )

    // accessTransformers.expose("src/main/resources/META-INF/accesstransformer.cfg")

    runs {
        configureEach {
            // JBR
            /*jvmArguments.addAll(
                "-XX:+AllowEnhancedClassRedefinition",
                "-XX:+IgnoreUnrecognizedVMOptions",
                "-XX:+AllowRedefinitionToAddDeleteMethods"
            )*/

            logLevel = Level.DEBUG
        }

        create("client") {
            client()
            gameDirectory = rootProject.file("run/client")
        }

        create("clientAuth") {
            client()
            gameDirectory = rootProject.file("run/client")

            mainClass = "net.covers1624.devlogin.DevLogin"
            programArguments.addAll("--launch_target", "cpw.mods.bootstraplauncher.BootstrapLauncher")
        }

        create("server") {
            server()
            gameDirectory = rootProject.file("run/server")
        }
    }

    mods {
        create("testmod") {
            sourceSet(sourceSets.main.get())
        }

        create("levels") {
            sourceSet(project(":levels").sourceSets.main.get())
        }

        create("rendering") {
            sourceSet(project(":rendering").sourceSets.main.get())
        }
    }
}

dependencies {
    implementation(project(":levels"))
    implementation(project(":rendering"))
}
