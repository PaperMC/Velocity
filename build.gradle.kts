import com.velocitypowered.script.VelocityCheckstylePlugin
import com.velocitypowered.script.VelocityPublishPlugin
import com.velocitypowered.script.VelocitySpotlessPlugin

plugins {
    `java-library`
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
        maven("https://s01.oss.sonatype.org/content/repositories/snapshots/") // adventure
        maven("https://repo.papermc.io/repository/maven-public/")
    }

    dependencies {
        testImplementation(rootProject.libs.junit)
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
