plugins {
    idea
    alias(forge.plugins.forgegradle)
    alias(forge.plugins.mixingradle)
}

evaluationDependsOn(":levels")
evaluationDependsOn(":rendering")

group = rootProject.group
version = rootProject.version
base.archivesName = "testmod"

java.toolchain.languageVersion = JavaLanguageVersion.of(17)

minecraft {
    mappings("official", forge.versions.minecraft.get())
    copyIdeResources = true

    accessTransformers.from(
        project(":rendering").file("src/main/resources/META-INF/accesstransformer.cfg"),
        file("src/main/resources/META-INF/accesstransformer.cfg")
    )

    runs {
        create("client") {
            workingDirectory(file("run/client"))
        }

        configureEach {
            // Recommended logging data for a userdev environment
            // The markers can be added/remove as needed separated by commas.
            // "SCAN": For mods scan.
            // "REGISTRIES": For firing of registry events.
            // "REGISTRYDUMP": For getting the contents of all registries.
            property("forge.logging.markers", "REGISTRIES")

            // Recommended logging level for the console
            // You can set various levels here.
            // Please read: https://stackoverflow.com/questions/2031163/when-to-use-the-different-log-levels
            property("forge.logging.console.level", "debug")

            ideaModule("Gander.testmod.main")

            mods {
                create("gander") {
                    source(sourceSets[SourceSet.MAIN_SOURCE_SET_NAME])
                }

                create("ganderlevels") {
                    source(project(":levels").sourceSets[SourceSet.MAIN_SOURCE_SET_NAME])
                }

                create("ganderrendering") {
                    source(project(":rendering").sourceSets[SourceSet.MAIN_SOURCE_SET_NAME])
                }
            }
        }
    }
}

mixin {
    add(sourceSets[SourceSet.MAIN_SOURCE_SET_NAME], "gander.refmap.json")
    config("gander.mixins.json")
}

dependencies {
    minecraft("net.minecraftforge:forge:${forge.versions.minecraft.get()}-${forge.versions.forge.get()}")
    annotationProcessor("org.spongepowered:mixin:${forge.versions.mixin.get()}:processor")

    implementation(project(":levels"))
    implementation(project(":rendering"))
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
    options.compilerArgs.addAll(arrayOf("-Xmaxerrs", "1000"))
}
