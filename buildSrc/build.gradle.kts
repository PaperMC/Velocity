plugins {
    `kotlin-dsl`
    checkstyle
    id("net.kyori.indra.publishing") version "2.0.6"
}

repositories {
    mavenCentral()
    maven("https://plugins.gradle.org/m2")
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
        register("velocity-publish") {
            id = "velocity-publish"
            implementationClass = "com.velocitypowered.script.VelocityPublishPlugin"
        }
    }
}
