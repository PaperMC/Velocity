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

        // Force a single blank line after every type declaration's opening brace,
        // e.g. after `class Foo {`, `interface Bar {`, `enum Baz {`, `record Qux(...) {`.
        custom("blankLineAfterTypeHeader", BlankLineAfterTypeHeaderStep())
    }
}
