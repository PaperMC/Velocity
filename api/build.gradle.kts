plugins {
    `java-library`
    `maven-publish`
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

    api(libs.slf4j)
    api(libs.guice)
    api(libs.checker.qual)
    api(libs.brigadier)
    api(libs.bundles.configurate)
    api(libs.caffeine)
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
        o.source = "8"

        o.links(
            "https://www.slf4j.org/apidocs/",
            "https://guava.dev/releases/${libs.guava.get().version}/api/docs/",
            "https://google.github.io/guice/api-docs/${libs.guice.get().version}/javadoc/",
            "https://docs.oracle.com/en/java/javase/11/docs/api/",
            "https://jd.advntr.dev/api/${libs.adventure.bom.get().version}/",
            "https://javadoc.io/doc/com.github.ben-manes.caffeine/caffeine"
        )

        // Disable the crazy super-strict doclint tool in Java 8
        o.addStringOption("Xdoclint:none", "-quiet")

        // Remove "undefined" from search paths when generating javadoc for a non-modular project (JDK-8215291)
        if (JavaVersion.current() >= JavaVersion.VERSION_1_9 && JavaVersion.current() < JavaVersion.VERSION_12) {
            o.addBooleanOption("-no-module-directories", true)
        }
    }
}
