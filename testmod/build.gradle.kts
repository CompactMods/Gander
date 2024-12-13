plugins {
    id("gander-convention")
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
