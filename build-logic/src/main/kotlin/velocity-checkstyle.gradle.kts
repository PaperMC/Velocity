plugins {
    checkstyle
}

extensions.configure<CheckstyleExtension> {
    configFile = rootProject.file("config/checkstyle/checkstyle.xml")
    maxErrors = 0
    maxWarnings = 0
    toolVersion = libs.checkstyle.get().version.toString()
}
