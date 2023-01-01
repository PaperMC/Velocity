rootProject.name = "velocity"
include(
        "api",
        "proxy",
        "native"
)
findProject(":api")?.name = "velocity-api"
findProject(":proxy")?.name = "velocity-proxy"
findProject(":native")?.name = "velocity-native"