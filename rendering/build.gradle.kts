plugins {
    idea
    alias(forge.plugins.forgegradle)
    alias(forge.plugins.mixingradle)
}

group = rootProject.group
version = rootProject.version
base.archivesName = "rendering"

java.toolchain.languageVersion = JavaLanguageVersion.of(17)

minecraft {
    mappings("official", forge.versions.minecraft.get())
    copyIdeResources = true
    accessTransformers.from(file("src/main/resources/META-INF/accesstransformer.cfg"))
}

mixin {
    add(sourceSets[SourceSet.MAIN_SOURCE_SET_NAME], "gander_render.refmap.json")
    config("gander_render.mixins.json")
}

dependencies {
    minecraft("net.minecraftforge:forge:${forge.versions.minecraft.get()}-${forge.versions.forge.get()}")
    annotationProcessor("org.spongepowered:mixin:${forge.versions.mixin.get()}:processor")
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
    options.compilerArgs.addAll(arrayOf("-Xmaxerrs", "1000"))
}

tasks.jar {
    manifest.attributes(
        "FMLModType" to "GAMELIBRARY",
        "Automatic-Module-Name" to "ganderrendering"
    )
}
