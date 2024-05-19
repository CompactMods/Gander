import net.neoforged.gradle.dsl.common.runs.run.Run

plugins {
    alias(neoforged.plugins.userdev).apply(false)
}

subprojects {
    afterEvaluate() {
        extensions.configure<JavaPluginExtension> {
            // toolchain.vendor.set(JvmVendorSpec)
            // toolchain.languageVersion.set(JavaLanguageVersion.of(21))
        }
    }
}
