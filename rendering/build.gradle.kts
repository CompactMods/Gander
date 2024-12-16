plugins {
    id("gander-convention")
}

mixin {
    add(sourceSets["main"], "gander_render.refmap.json")
    config("gander_render.mixins.json")
}
