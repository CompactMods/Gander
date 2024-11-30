import dev.compactmachines.GanderConstants
import org.slf4j.event.Level

plugins {
    id("java-library")
    id("idea")
    id("org.jetbrains.gradle.plugin.idea-ext")
    id("net.neoforged.moddev.legacyforge")
}

// main convention script which sets up a basic MDG workspace
// pulling in version data from GanderConstants since version catalogs
// from the main outer project are not accessible here

group = GanderConstants.GROUP
version = GanderConstants.VERSION

base {
    archivesName = project.name
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

    runs.configureEach {
        // set up basic common run config properties
        // due to using convention this can be override in each run config
        logLevel.convention(Level.DEBUG)
        sourceSet.convention(sourceSets[SourceSet.MAIN_SOURCE_SET_NAME])
        loadedMods.convention(mods) // all registered mods
        gameDirectory.convention(type.map { layout.projectDirectory.dir("run/$it") })

        // using '.map' adds args as a 'provider'
        // making them lazily evaluated later
        // when 'type' actually exists
        //
        // adds jbr jvm args only for client/server run types
        jvmArguments.addAll(type.map {
            if(GanderConstants.IS_CI || !it.equals("client") || !it.equals("server"))
                return@map emptyList<String>()

            return@map listOf(
                "-XX:+AllowEnhancedClassRedefinition",
                "-XX:+IgnoreUnrecognizedVMOptions",
                "-XX:+AllowRedefinitionToAddDeleteMethods"
            )
        })
    }
}

dependencies {
    // not included with MDG currently
    // does not affect runtime or production
    // safe to have at compile time with no issues
    compileOnly("org.jetbrains:annotations:26.0.1")
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
