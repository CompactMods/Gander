plugins {
    id("gander-convention")
}

tasks.jar {
    manifest.from(file("src/main/resources/META-INF/MANIFEST.MF"))
}
