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
    api("io.hotmoka:toml4j:0.7.3")

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
        val o = options as StandardJavadocDocletOptions
        o.encoding = "UTF-8"
        o.source = "21"

        o.use()
        o.links(
            "https://www.javadocs.dev/org.slf4j/slf4j-api/${libs.slf4j.get().version}/",
            "https://guava.dev/releases/${libs.guava.get().version}/api/docs/",
            "https://google.github.io/guice/api-docs/${libs.guice.get().version}/javadoc/",
            "https://docs.oracle.com/en/java/javase/21/docs/api/",
            "https://jd.advntr.dev/api/${libs.adventure.bom.get().version}/",
            "https://jd.advntr.dev/text-minimessage/${libs.adventure.bom.get().version}/",
            "https://jd.advntr.dev/key/${libs.adventure.bom.get().version}/",
            "https://www.javadocs.dev/com.github.ben-manes.caffeine/caffeine/${libs.caffeine.get().version}/",
        )

        o.tags(
            "apiNote:a:API Note:",
            "implSpec:a:Implementation Requirements:",
            "implNote:a:Implementation Note:",
            "sinceMinecraft:a:Since Minecraft:"
        )
    }
}
