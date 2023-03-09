pluginManagement {
    repositories {
        gradlePluginPortal()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.4.0"
}

rootProject.name = "velocity"
include(
        "api",
        "proxy",
        "native"
)
findProject(":api")?.name = "velocity-api"
findProject(":proxy")?.name = "velocity-proxy"
findProject(":native")?.name = "velocity-native"