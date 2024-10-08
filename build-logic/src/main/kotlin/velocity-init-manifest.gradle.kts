import net.kyori.indra.git.IndraGitExtension
import org.gradle.jvm.tasks.Jar
import org.gradle.kotlin.dsl.withType
import java.time.Instant

plugins {
    id("net.kyori.indra.git")
}

tasks.withType<Jar> {
    manifest {
        val indraGit = project.extensions.getByType(IndraGitExtension::class.java)
        val buildNumber = System.getenv("BUILD_NUMBER")
        val gitHash = indraGit.commit()?.name?.substring(0, 8) ?: "unknown"
        val gitBranch = indraGit.branchName() ?: "unknown"
        val velocityVersion = project.version.toString()
        val velocityVersionButWithoutTheDashSnapshot = velocityVersion.replace("-SNAPSHOT", "")
        val implementationVersion = "$velocityVersion-${buildNumber ?: "DEV"}-$gitHash"
        val velocityHumanVersion: String =
            if (project.version.toString().endsWith("-SNAPSHOT")) {
                if (buildNumber == null) {
                    "${project.version} (git-$gitHash)"
                } else {
                    "${project.version} (git-$gitHash-b$buildNumber)"
                }
            } else {
                archiveVersion.get()
            }
        attributes["Implementation-Title"] = "Velocity"
        attributes["Implementation-Vendor"] = "Velocity Contributors"
        attributes["Multi-Release"] = "true"
        attributes["Specification-Version"] = velocityHumanVersion
        attributes["Implementation-Version"] = velocityVersionButWithoutTheDashSnapshot
        attributes["Brand-Id"] = "papermc:velocity"
        attributes["Brand-Name"] = "Velocity"
        attributes["Build-Number"] = (buildNumber ?: "")
        attributes["Build-Time"] = Instant.now().toString()
        attributes["Git-Branch"] = gitBranch
        attributes["Git-Commit"] = gitHash
    }
}
