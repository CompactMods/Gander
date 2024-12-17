plugins {
    id("gander-convention")
}

mixin {
    add(sourceSets["main"], "gander_levels.refmap.json")
    config("gander_levels.mixins.json")
}
