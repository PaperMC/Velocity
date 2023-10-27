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

val gsonVersion: String by project.extra
val guiceVersion: String by project.extra
val guavaVersion: String by project.extra
val adventureVersion: String by project.extra
val slf4jVersion: String by project.extra
val checkerFrameworkVersion: String by project.extra
val configurateVersion: String by project.extra
val caffeineVersion: String by project.extra

dependencies {
    api(libs.gson)
    api(libs.guava)

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
                "https://guava.dev/releases/$guavaVersion/api/docs/",
                "https://google.github.io/guice/api-docs/$guiceVersion/javadoc/",
                "https://docs.oracle.com/en/java/javase/17/docs/api/",
                "https://jd.advntr.dev/api/$adventureVersion/",
                "https://javadoc.io/doc/com.github.ben-manes.caffeine/caffeine/3.1.5/"
        )

        // Disable the crazy super-strict doclint tool in Java 8
        o.addStringOption("Xdoclint:none", "-quiet")
    }
}
