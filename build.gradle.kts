import com.velocitypowered.script.VelocityCheckstylePlugin
import com.velocitypowered.script.VelocityPublishPlugin
import com.velocitypowered.script.VelocitySpotlessPlugin

plugins {
    `java-library`
}

val junitVersion: String by project.extra

allprojects {
    group = "com.velocitypowered"
    version = "3.2.0-SNAPSHOT"
}

subprojects {
    apply<JavaLibraryPlugin>()

    apply<VelocityCheckstylePlugin>()
    apply<VelocityPublishPlugin>()
    apply<VelocitySpotlessPlugin>()

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
                junitXml.required.set(true)
            }
        }
    }
}
