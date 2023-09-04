plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.4.0"
}

rootProject.name = "velocity"

sequenceOf(
    "api",
    "proxy",
    "native",
).forEach {
    val project = ":velocity-$it"
    include(project)
    project(project).projectDir = file(it)
}
