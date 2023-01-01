import com.velocitypowered.script.VelocityCheckstylePlugin
import org.cadixdev.gradle.licenser.Licenser

plugins {
    `java-library`
    id("com.github.johnrengelman.shadow") version "7.1.0"
    id("org.cadixdev.licenser") version "0.6.1"
}

val junitVersion: String by project.extra

subprojects {
    group = "com.velocitypowered"
    version = "3.1.2-SNAPSHOT"

    apply<JavaLibraryPlugin>()
    apply<Licenser>()

    apply<VelocityCheckstylePlugin>()

    java {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(11))
        }
    }

    repositories {
        mavenCentral()
        // kyoripowered
        maven("https://oss.sonatype.org/content/groups/public/")
        // velocity
        maven("https://nexus.velocitypowered.com/repository/maven-public/")
    }
    dependencies {
        testImplementation("org.junit.jupiter:junit-jupiter:$junitVersion")
    }

    tasks {
        test {
            useJUnitPlatform()
            reports {
                junitXml.isEnabled = true
            }
        }
    }
}
