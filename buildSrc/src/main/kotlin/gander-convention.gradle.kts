import dev.compactmachines.GanderConstants
import org.slf4j.event.Level

var envVersion: String = System.getenv("VERSION") ?: "9.9.9"
if (envVersion.startsWith("v"))
    envVersion = envVersion.trimStart('v')

plugins {
    id("java-library")
    id("idea")
    id("org.jetbrains.gradle.plugin.idea-ext")
    id("net.neoforged.moddev.legacyforge")
    id("maven-publish")
}

// main convention script which sets up a basic MDG workspace
// pulling in version data from GanderConstants since version catalogs
// from the main outer project are not accessible here

base {
    archivesName = project.name
    group = "dev.compactmods.gander"
    version = envVersion

    libsDirectory.convention(rootProject.layout.projectDirectory.dir("libs/${project.name}"))
}

idea.module {
    if(!GanderConstants.IS_CI) {
        isDownloadSources = true
        isDownloadJavadoc = true
    }

    excludeDirs.addAll(files(
        ".gradle",
        "build",
        "run"
    ))
}

java {
    toolchain {
        languageVersion.convention(GanderConstants.JAVA_VERSION)
        vendor.convention(GanderConstants.JAVA_VENDOR)
    }
}

neoForge {
    version.convention(GanderConstants.FORGE_VERSION)
    // not currently usable since the AT included in legacy forge
    // does not itself validate, many unused/nonexistent AT entries exist in it
    // validateAccessTransformers.convention(true)

    parchment {
        minecraftVersion.convention(GanderConstants.PARCHMENT_VERSION)
        mappingsVersion.convention(GanderConstants.PARCHMENT_MAPPINGS)
    }

    // apply this modules AT file if it exists
    // also mark for publishing with maven publication
    val atFile = file("src/main/resources/META-INF/accesstransformer.cfg")

    if(atFile.exists()) {
        accessTransformers {
            from(atFile)
            publish(atFile)
        }
    }
}

tasks.jar {
    manifest.from(file("src/main/resources/META-INF/MANIFEST.MF"))
}

dependencies {
    // not included with MDG currently
    // does not affect runtime or production
    // safe to have at compile time with no issues
    compileOnly("org.jetbrains:annotations:26.0.1")
    annotationProcessor("org.spongepowered:mixin:0.8.5:processor")
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
    options.release.convention(GanderConstants.JAVA_VERSION.asInt())

    if(!GanderConstants.IS_CI) {
        options.compilerArgs.addAll(arrayOf("-Xmaxerrs", "9000"))
    }
}

javaToolchains.compilerFor {
    languageVersion.convention(GanderConstants.JAVA_VERSION)
    vendor.convention(GanderConstants.JAVA_VENDOR)
}

val PACKAGES_URL = System.getenv("GH_PKG_URL") ?: "https://maven.pkg.github.com/compactmods/gander"
publishing {
    publications.register<MavenPublication>(project.name) {
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
