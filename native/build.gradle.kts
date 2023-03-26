plugins {
    `java-library`
    `maven-publish`
}

dependencies {
    implementation(libs.guava)
    implementation(libs.netty.handler)
    implementation(libs.checker.qual)
}
