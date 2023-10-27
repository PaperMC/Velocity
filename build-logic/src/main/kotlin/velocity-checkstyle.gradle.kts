plugins {
    checkstyle
}

extensions.configure<CheckstyleExtension> {
    configFile = rootProject.file("config/checkstyle/checkstyle.xml")
    maxErrors = 0
    maxWarnings = 0
    toolVersion = libs.checkstyle.get().version.toString()
    configProperties["org.checkstyle.google.suppressionfilter.config"] =
            rootProject.file("config/checkstyle/checkstyle-suppressions.xml")
}
