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

val gsonVersion: String by project.extra
val guiceVersion: String by project.extra
val guavaVersion: String by project.extra
val adventureVersion: String by project.extra
val slf4jVersion: String by project.extra
val checkerFrameworkVersion: String by project.extra
val configurateVersion: String by project.extra

dependencies {
    api("com.google.code.gson:gson:$gsonVersion")
    api("com.google.guava:guava:$guavaVersion")

    // DEPRECATED: Will be removed in Velocity Polymer
    api("com.moandjiezana.toml:toml4j:0.7.2")

    api(platform("net.kyori:adventure-bom:${adventureVersion}"))
    api("net.kyori:adventure-api")
    api("net.kyori:adventure-text-serializer-gson")
    api("net.kyori:adventure-text-serializer-legacy")
    api("net.kyori:adventure-text-serializer-plain")
    api("net.kyori:adventure-text-minimessage")

    api("org.slf4j:slf4j-api:$slf4jVersion")
    api("com.google.inject:guice:$guiceVersion")
    api("org.checkerframework:checker-qual:${checkerFrameworkVersion}")
    api("com.velocitypowered:velocity-brigadier:1.0.0-SNAPSHOT")

    api("org.spongepowered:configurate-hocon:${configurateVersion}")
    api("org.spongepowered:configurate-yaml:${configurateVersion}")
    api("org.spongepowered:configurate-gson:${configurateVersion}")
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
                "https://docs.oracle.com/en/java/javase/11/docs/api/",
                "https://jd.adventure.kyori.net/api/$adventureVersion/"
        )

        // Disable the crazy super-strict doclint tool in Java 8
        o.addStringOption("Xdoclint:none", "-quiet")

        // Remove "undefined" from search paths when generating javadoc for a non-modular project (JDK-8215291)
        if (JavaVersion.current() >= JavaVersion.VERSION_1_9 && JavaVersion.current() < JavaVersion.VERSION_12) {
            o.addBooleanOption("-no-module-directories", true)
        }
    }
}
