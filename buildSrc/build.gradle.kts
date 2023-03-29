@Suppress("DSL_SCOPE_VIOLATION") // fixed in Gradle 8.1
plugins {
    `kotlin-dsl`
    checkstyle
    alias(libs.plugins.indra.publishing)
    alias(libs.plugins.spotless)
}

dependencies {
    implementation("com.diffplug.spotless:spotless-plugin-gradle:${libs.plugins.spotless.get().version}")
}

gradlePlugin {
    plugins {
        register("set-manifest-impl-version") {
            id = "set-manifest-impl-version"
            implementationClass = "com.velocitypowered.script.SetManifestImplVersionPlugin"
        }
        register("velocity-checkstyle") {
            id = "velocity-checkstyle"
            implementationClass = "com.velocitypowered.script.VelocityCheckstylePlugin"
        }
        register("velocity-spotless") {
            id = "velocity-spotless"
            implementationClass = "com.velocitypowered.script.VelocitySpotlessPlugin"
        }
        register("velocity-publish") {
            id = "velocity-publish"
            implementationClass = "com.velocitypowered.script.VelocityPublishPlugin"
        }
    }
}

spotless {
    kotlin {
        licenseHeaderFile(project.rootProject.file("../HEADER.txt"))
    }
}