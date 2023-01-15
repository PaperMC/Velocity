plugins {
    `kotlin-dsl`
    checkstyle
    id("net.kyori.indra.publishing") version "2.0.6"
    id("com.diffplug.spotless") version "6.12.0"
}

repositories {
    mavenCentral()
    maven("https://plugins.gradle.org/m2")
}

dependencies {
    implementation("com.diffplug.spotless:spotless-plugin-gradle:6.12.0")
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