plugins {
    java
    `maven-publish`
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
            groupId = "com.velocitypowered"
            artifactId = "velocity-api"
            version = "3.4.0-LMC" // or project.version
        }
    }

    repositories {
        // PaperMC (releases/snapshots auto-selected by version suffix)
        maven {
            name = if (project.version.toString().endsWith("SNAPSHOT")) "paperSnapshots" else "paper"
            val base = "https://artifactory.papermc.io/artifactory"
            url = uri(
                if (project.version.toString().endsWith("SNAPSHOT")) "$base/snapshots/"
                else "$base/releases/"
            )
            credentials(PasswordCredentials::class)
        }

        // LifestealMC private repo
        maven {
            name = "lifestealmc"
            url = uri("https://repo.lifestealmc.com/private")
            credentials {
                username = (findProperty("repoUser") as String?) ?: ""
                password = (findProperty("repoPass") as String?) ?: ""
            }
        }
    }
}
