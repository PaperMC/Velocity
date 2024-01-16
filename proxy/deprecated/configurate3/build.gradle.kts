plugins {
    alias(libs.plugins.shadow)
}

dependencies {
    implementation(libs.bundles.configurate3)
}

tasks.shadowJar {
    exclude("com/google/**")
    exclude("com/typesafe/**")
    exclude("org/yaml/**")
}
