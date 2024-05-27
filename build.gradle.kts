plugins {
    `java-library`
    id("velocity-checkstyle") apply false
    id("velocity-spotless") apply false
    alias(libs.plugins.ideaExt)
}

subprojects {
    apply<JavaLibraryPlugin>()

    apply(plugin = "velocity-checkstyle")
    apply(plugin = "velocity-spotless")
    apply(plugin = "org.jetbrains.gradle.plugin.idea-ext")

    java {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(17))
        }
    }

    dependencies {
        testImplementation(rootProject.libs.junit)
    }

    tasks {
        test {
            useJUnitPlatform()
            reports {
                junitXml.required.set(true)
            }
        }
    }

    idea {
        if (project != null) {
            (project as ExtensionAware).extensions["settings"].run {
                (this as ExtensionAware).extensions.getByType(org.jetbrains.gradle.ext.ActionDelegationConfig::class).run {
                    delegateBuildRunToGradle = false
                    testRunner = org.jetbrains.gradle.ext.ActionDelegationConfig.TestRunner.PLATFORM
                }
                extensions.getByType(org.jetbrains.gradle.ext.IdeaCompilerConfiguration::class).run {
                    addNotNullAssertions = false
                    useReleaseOption = JavaVersion.current().isJava10Compatible
                    parallelCompilation = true
                }
            }
        }
    }
}
