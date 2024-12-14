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

tasks.register("publish") {
    dependsOn(project(":core").tasks.named("publish"))
    dependsOn(project(":levels").tasks.named("publish"))
    dependsOn(project(":rendering").tasks.named("publish"))
    dependsOn(project(":ui").tasks.named("publish"))
}
