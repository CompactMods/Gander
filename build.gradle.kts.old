import net.neoforged.gradle.dsl.common.runs.run.Run

plugins {
    alias(neoforged.plugins.userdev).apply(false)
}

subprojects {
    configurations {
        val minecraft = create("minecraft") {
            isCanBeConsumed = false
            isCanBeResolved = true

            // N.B. this is important until https://github.com/neoforged/FancyModLoader/issues/124 is resolved
            isTransitive = false
        }

        val mod = create("mod") {
            isCanBeConsumed = false
            isCanBeResolved = true

            isTransitive = false
        }

        register("compileClasspath") {
            extendsFrom(minecraft)
            extendsFrom(mod)
        }
        register("runtimeClasspath") {
            extendsFrom(minecraft)
        }
    }
}

subprojects {
    afterEvaluate {
        extensions.configure<JavaPluginExtension> {
            // toolchain.vendor.set(JvmVendorSpec)
            toolchain.languageVersion.set(JavaLanguageVersion.of(21))
        }

        this.extensions.configure<NamedDomainObjectContainer<Run>> {
            configureEach {
                dependencies {
                    runtime(configurations.getByName("mod"))
                }
            }
        }
    }
}
