plugins {
    id("gander-convention")
}

// sets up MDG workspace and includes all other gander modules

// 'modules' conflicts in dependencies block
// list containing all of ganders modules
// aka every project other than this one (testmod)
//
// these modules are auto included everywhere needed
// so all thats needed to add a new one is include one in settings.gradle
val gModules = rootProject.childProjects.map { it.value }.filter { it != project }
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

dependencies {
    gModules.forEach(::implementation)
}
