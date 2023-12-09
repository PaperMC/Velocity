plugins {
    `java-library`
    id("velocity-checkstyle") apply false
    id("velocity-spotless") apply false
}

subprojects {
    apply<JavaLibraryPlugin>()

    apply(plugin = "velocity-checkstyle")
    apply(plugin = "velocity-spotless")

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
}
