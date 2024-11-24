plugins {
    idea
    alias(forge.plugins.forgegradle)
    alias(forge.plugins.mixingradle)
}

group = rootProject.group
version = rootProject.version
base.archivesName = "levels"

java.toolchain.languageVersion = JavaLanguageVersion.of(17)

minecraft {
    mappings("official", forge.versions.minecraft.get())
    copyIdeResources = true
}

mixin {
    add(sourceSets[SourceSet.MAIN_SOURCE_SET_NAME], "gander_levels.refmap.json")
    config("gander_levels.mixins.json")
}

dependencies {
    minecraft("net.minecraftforge:forge:${forge.versions.minecraft.get()}-${forge.versions.forge.get()}")
    annotationProcessor("org.spongepowered:mixin:${forge.versions.mixin.get()}:processor")
}

tasks.jar {
    manifest.attributes(
        "FMLModType" to "GAMELIBRARY",
        "Automatic-Module-Name" to "ganderlevels"
    )
}
