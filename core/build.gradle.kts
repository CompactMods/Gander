val isRelease: Boolean = (System.getenv("RELEASE") ?: "false").equals("true", true)

plugins {
    id("gander-convention")
}
