plugins {
    `java-library`
    `maven-publish`
    id("velocity-publish")
    kotlin("jvm") version "1.9.22"
}

val apKotlinOnly by configurations.creating
val apAndMain by configurations.creating

val ap by sourceSets.creating

tasks.named("compileApKotlin", org.jetbrains.kotlin.gradle.tasks.KotlinCompile::class) {
    libraries.from(apKotlinOnly)
}

// Make IDEA happy -- eclipse doesn't handle Kotlin anyways
if (System.getProperty("idea.sync.active").toBoolean()) {
    configurations.named("apImplementation") {
        extendsFrom(apKotlinOnly)
    }
}

configurations {
    api { extendsFrom(apAndMain) }
    named(ap.apiConfigurationName) { extendsFrom(apAndMain) }

    // Expose AP to other subprojects
    sequenceOf(apiElements, runtimeElements).forEach {
        it {
            outgoing.variants.named("classes") {
                val classesDirs = ap.output.classesDirs
                classesDirs.forEach { dir ->
                    artifact(dir) {
                        type = ArtifactTypeDefinition.JVM_CLASS_DIRECTORY
                        builtBy(classesDirs.buildDependencies)
                    }
                }
            }
        }
    }
}

kotlin {
    val minimumKotlin = "1.7"
    target.compilations.configureEach {
        kotlinOptions {
            apiVersion = minimumKotlin
            languageVersion = minimumKotlin
            jvmTarget = "17"
        }
    }
}

java {
    withJavadocJar()
    withSourcesJar()
}

dependencies {
    compileOnlyApi(libs.jspecify)

    apAndMain(libs.gson)
    apAndMain(libs.guava)

    // DEPRECATED: Will be removed in Velocity Polymer
    api("com.moandjiezana.toml:toml4j:0.7.2")

    api(platform(libs.adventure.bom))
    api("net.kyori:adventure-api")
    api("net.kyori:adventure-text-serializer-gson")
    api("net.kyori:adventure-text-serializer-legacy")
    api("net.kyori:adventure-text-serializer-plain")
    api("net.kyori:adventure-text-minimessage")
    api("net.kyori:adventure-text-logger-slf4j")
    api("net.kyori:adventure-text-serializer-ansi")

    api(libs.snakeyaml)

    api(libs.slf4j)
    api(libs.guice)
    apAndMain(libs.checker.qual)
    api(libs.brigadier)
    api(libs.bundles.configurate4)
    api(libs.caffeine)
    implementation(ap.output)
    apKotlinOnly(libs.kspApi)
    apKotlinOnly(kotlin("stdlib-jdk8", "1.7.22"))
}

tasks {
    jar {
        manifest {
            attributes["Automatic-Module-Name"] = "com.velocitypowered.api"
        }
        from(ap.output)
    }
    withType<Javadoc> {
        val o = options as StandardJavadocDocletOptions
        o.encoding = "UTF-8"
        o.source = "8"

        o.links(
            "https://www.slf4j.org/apidocs/",
            "https://guava.dev/releases/${libs.guava.get().version}/api/docs/",
            "https://google.github.io/guice/api-docs/${libs.guice.get().version}/javadoc/",
            "https://docs.oracle.com/en/java/javase/17/docs/api/",
            "https://jd.advntr.dev/api/${libs.adventure.bom.get().version}/",
            "https://javadoc.io/doc/com.github.ben-manes.caffeine/caffeine"
        )

        // Disable the crazy super-strict doclint tool in Java 8
        o.addStringOption("Xdoclint:none", "-quiet")
    }
}
