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

    tasks.withType<JavaCompile> {
        options.encoding = Charsets.UTF_8.name()
        options.release.set(11)
    }

    tasks.withType<Javadoc> {
        options.encoding = Charsets.UTF_8.name()
    }

    @Suppress("UnstableApiUsage")
    tasks.withType<ProcessResources> {
        filteringCharset = Charsets.UTF_8.name()
    }

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
