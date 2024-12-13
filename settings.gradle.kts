plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}

// include more modules here
include("levels")
include("rendering")
include("ui")

// project used for actual testing in dev
// modules are auto hooked into to this project
include("testmod")

rootProject.name = "Gander"
include("core")
