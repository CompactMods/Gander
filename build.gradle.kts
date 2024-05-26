plugins {
    alias(libraries.plugins.idea.ext)
}

idea.module.excludeDirs.addAll(files(
    ".gradle",
    ".idea",
    "build",
    "gradle",
    "run"
))
