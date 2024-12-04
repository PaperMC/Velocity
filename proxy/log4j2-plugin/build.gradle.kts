tasks.withType<JavaCompile> {
    options.compilerArgs.addAll(listOf(
        "-Alog4j.graalvm.groupId=io.github.bivashy ",
        "-Alog4j.graalvm.artifactId=velocity-proxy-log4j2-plugin"
    ))
}

dependencies {
    implementation(libs.bundles.log4j)
    annotationProcessor(libs.log4j.core)
}