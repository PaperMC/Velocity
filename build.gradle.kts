plugins {
    `java-library`
}

subprojects {
    apply<JavaLibraryPlugin>()

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
