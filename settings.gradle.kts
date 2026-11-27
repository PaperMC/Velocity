@file:Suppress("UnstableApiUsage")

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenCentral()
        maven("https://repo.papermc.io/repository/maven-public/")
        // TODO: remove once repo.papermc.io mirrors GeyserMC's Configurate fork
        maven("https://repo.opencollab.dev/maven-releases/") {
            content { includeVersionByRegex("org\\.spongepowered", "configurate-.*", ".*-GeyserMC") }
        }
    }
}

pluginManagement {
    includeBuild("build-logic")
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "velocity"

sequenceOf(
    "api",
    "native",
    "proxy",
).forEach {
    val project = ":velocity-$it"
    include(project)
    project(project).projectDir = file(it)
}

// Include Configurate 3
val deprecatedConfigurateModule = ":deprecated-configurate3"
include(deprecatedConfigurateModule)
project(deprecatedConfigurateModule).projectDir = file("proxy/deprecated/configurate3")
