plugins {
    `java-library`
    `maven-publish`
}

val guavaVersion: String by project.extra
val nettyVersion: String by project.extra
val checkerFrameworkVersion: String by project.extra

dependencies {
    implementation("com.google.guava:guava:${guavaVersion}")
    implementation("io.netty:netty-handler:${nettyVersion}")
    implementation("org.checkerframework:checker-qual:${checkerFrameworkVersion}")
}