plugins {
    java
    `maven-publish`
}

extensions.configure<PublishingExtension> {
    repositories {
        if (project.properties["generic.publish"] == "true") {
            maven(url = (project.findProperty("generic.url") ?: "") as String) {
                name = "Generic"
                credentials(PasswordCredentials::class) {
                    username = (project.findProperty("generic.auth.username") ?: "") as String
                    password = (project.findProperty("generic.auth.password") ?: "") as String
                }
            }
        }
    }
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
            pom {
                name.set("Velocity")
                description.set("The modern, next-generation Minecraft server proxy")
                url.set("https://papermc.io/software/velocity")
                scm {
                    url.set("https://github.com/PaperMC/Velocity")
                    connection.set("scm:git:https://github.com/PaperMC/Velocity.git")
                    developerConnection.set("scm:git:https://github.com/PaperMC/Velocity.git")
                }
            }
        }
    }
}
