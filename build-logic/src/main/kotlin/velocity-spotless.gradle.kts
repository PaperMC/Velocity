import com.diffplug.gradle.spotless.SpotlessExtension
import com.diffplug.gradle.spotless.SpotlessPlugin

apply<SpotlessPlugin>()

extensions.configure<SpotlessExtension> {
    java {
        if (project.name == "velocity-api") {
            licenseHeaderFile(file("HEADER.txt"))
            targetExclude("**/java/com/velocitypowered/api/util/Ordered.java")
        } else {
            licenseHeaderFile(rootProject.file("HEADER.txt"))
        }
        removeUnusedImports()
    }
    plugins.withId("org.jetbrains.kotlin.jvm") {
        kotlin {
            if (project.name == "velocity-api") {
                licenseHeaderFile(project.file("HEADER.txt"))
            } else {
                licenseHeaderFile(project.rootProject.file("HEADER.txt"))
            }
            ktfmt()
                .kotlinlangStyle()
                .configure {
                    it.setMaxWidth(100)
                    it.setBlockIndent(2)
                    it.setContinuationIndent(4)
                    it.setRemoveUnusedImport(true)
                }
        }
    }
}
