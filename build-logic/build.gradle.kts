plugins {
    `kotlin-dsl`
    alias(libs.plugins.spotless)
}

dependencies {
    // this is OK as long as the same version catalog is used in the main build and build-logic
    // see https://github.com/gradle/gradle/issues/15383#issuecomment-779893192
    implementation(files(libs.javaClass.superclass.protectionDomain.codeSource.location))
    implementation("com.diffplug.spotless:spotless-plugin-gradle:${libs.plugins.spotless.get().version}")
}

spotless {
    kotlin {
        licenseHeaderFile(rootProject.file("../HEADER.txt"))
    }
}
