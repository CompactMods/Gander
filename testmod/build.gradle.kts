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

javaToolchains.compilerFor {
    languageVersion.convention(GanderConstants.JAVA_VERSION)
    vendor.convention(GanderConstants.JAVA_VENDOR)
}

// sets up MDG workspace and includes all other gander modules
val gModules = listOf(
    project(":core"),
    project(":levels"),
    project(":rendering"),
    project(":ui")
)

gModules.map { it.path }.forEach(::evaluationDependsOn)

neoForge {
    version = GanderConstants.FORGE_VERSION
    // not currently usable since the AT included in legacy forge
    // does not itself validate, many unused/nonexistent AT entries exist in it
    // validateAccessTransformers.convention(true)

    parchment {
        minecraftVersion = GanderConstants.PARCHMENT_VERSION
        mappingsVersion = GanderConstants.PARCHMENT_MAPPINGS
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

    mods {
        create(SourceSet.MAIN_SOURCE_SET_NAME) {
            sourceSet(sourceSets[SourceSet.MAIN_SOURCE_SET_NAME])
        }

        gModules.forEach {
            create(it.name) {
                sourceSet(it.sourceSets[SourceSet.MAIN_SOURCE_SET_NAME])
            }
        }
    }

    // apply at's from other modules, if they exist
    val moduleAts = gModules.map { it.file("src/main/resources/META-INF/accesstransformer.cfg") }.filter { it.exists() }

    if(moduleAts.isNotEmpty()) {
        accessTransformers.from(moduleAts)
    }

    runs {
        configureEach {
            // set up basic common run config properties
            // due to using convention this can be override in each run config
            logLevel = Level.DEBUG
            sourceSet = sourceSets[SourceSet.MAIN_SOURCE_SET_NAME]
            loadedMods = mods // all registered mod
            gameDirectory = type.map { layout.projectDirectory.dir("run/$it") }

            // using '.map' adds args as a 'provider'
            // making them lazily evaluated later
            // when 'type' actually exists
            //
            // adds jbr jvm args only for client/server run types
            jvmArguments.addAll(type.map {
                if(GanderConstants.IS_CI || (it != "client" && it != "server"))
                    return@map emptyList<String>()

                return@map listOf(
                    "-XX:+AllowEnhancedClassRedefinition",
                    "-XX:+IgnoreUnrecognizedVMOptions",
                    "-XX:+AllowRedefinitionToAddDeleteMethods"
                )
            })
        }

        create("client") {
            client()
        }

        create("server") {
            server()
        }
    }
}

repositories {
    exclusiveContent {
        forRepository { maven("https://cursemaven.com") }
        filter{ includeGroup("curse.maven") }
    }
}

dependencies {
    gModules.forEach(::implementation)

    modImplementation("curse.maven:chicken-chunks-1-8-243883:5292574")
    modImplementation("curse.maven:codechicken-lib-1-8-242818:5753868")
}

mixin {
    config("gander_render.mixins.json")
    config("gander_levels.mixins.json")
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

    if(!GanderConstants.IS_CI) {
        options.compilerArgs.addAll(arrayOf("-Xmaxerrs", "9000"))
    }
}
