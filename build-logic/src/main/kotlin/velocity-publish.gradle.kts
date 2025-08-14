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
            version = "3.4.0-LMC"
        }
    }
    repositories {
        maven {
            name = "lifestealmc"
            url = uri("https://repo.lifestealmc.com/private")

            credentials {
                username = findProperty("repoUser") as String?: ""
                password = findProperty("repoPass") as String?: ""
            }
        }
    }
}
