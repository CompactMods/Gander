plugins {
    id("org.jetbrains.gradle.plugin.idea-ext")
}

idea.module.excludeDirs.addAll(files(
    ".github",
    ".gradle",
    ".idea",
    "gradle",
    "libs"
))
