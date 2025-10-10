plugins {
    java
    `maven-publish`
}

extensions.configure<PublishingExtension> {
    repositories {
        maven {
            credentials(PasswordCredentials::class.java)

            name = if (version.toString().endsWith("SNAPSHOT")) "paperSnapshots" else "paper" // "paper" is seemingly not defined
            val base = "https://artifactory.papermc.io/artifactory"
            val releasesRepoUrl = "$base/releases/"
            val snapshotsRepoUrl = "$base/snapshots/"
            setUrl(if (version.toString().endsWith("SNAPSHOT")) snapshotsRepoUrl else releasesRepoUrl)
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
