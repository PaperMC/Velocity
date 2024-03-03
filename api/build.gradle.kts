plugins {
    `java-library`
    `maven-publish`
    id("velocity-publish")
}

java {
    withJavadocJar()
    withSourcesJar()

    sourceSets["main"].java {
        srcDir("src/ap/java")
    }

    sourceSets["main"].resources {
        srcDir("src/ap/resources")
    }
}

dependencies {
    compileOnlyApi(libs.jspecify)

    api(libs.gson)
    api(libs.guava)

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
    api(libs.checker.qual)
    api(libs.brigadier)
    api(libs.bundles.configurate4)
    api(libs.caffeine)

    compileOnly(libs.auto.service.annotations)
    annotationProcessor(libs.auto.service)
}

tasks {
    jar {
        manifest {
            attributes["Automatic-Module-Name"] = "com.velocitypowered.api"
        }
    }
    withType<Javadoc> {
        exclude("com/velocitypowered/api/plugin/ap/**")

        val o = options as StandardJavadocDocletOptions
        o.encoding = "UTF-8"
        o.source = "17"

        o.links(
            "https://www.slf4j.org/apidocs/",
            "https://guava.dev/releases/${libs.guava.get().version}/api/docs/",
            "https://google.github.io/guice/api-docs/${libs.guice.get().version}/javadoc/",
            "https://docs.oracle.com/en/java/javase/17/docs/api/",
            "https://jd.advntr.dev/api/${libs.adventure.bom.get().version}/",
            "https://javadoc.io/doc/com.github.ben-manes.caffeine/caffeine"
        )

        o.tags(
            "apiNote:a:API Note:",
            "implSpec:a:Implementation Requirements:",
            "implNote:a:Implementation Note:"
        )

        // Disable the crazy super-strict doclint tool in Java 8
        o.addStringOption("Xdoclint:none", "-quiet")
    }
}
