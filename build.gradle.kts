plugins {
    id("org.jetbrains.gradle.plugin.idea-ext") version ("1.1.9")
}

idea.module.excludeDirs.addAll(files(
    ".github",
    ".gradle",
    ".idea",
    "gradle",
    "libs"
))
