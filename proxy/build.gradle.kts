import com.github.jengelman.gradle.plugins.shadow.transformers.Log4j2PluginsCacheFileTransformer
import io.papermc.fill.model.BuildChannel

plugins {
    application
    id("velocity-init-manifest")
    alias(libs.plugins.shadow)
    alias(libs.plugins.fill)
}

application {
    mainClass.set("com.velocitypowered.proxy.Velocity")
    applicationDefaultJvmArgs += listOf("-Dvelocity.packet-decode-logging=true")
}

tasks {
    jar {
        manifest {
            attributes["Implementation-Title"] = "Velocity"
            attributes["Implementation-Vendor"] = "Velocity Contributors"
            attributes["Multi-Release"] = "true"
        }
    }

    shadowJar {
        filesMatching("META-INF/org/apache/logging/log4j/core/config/plugins/**") {
            duplicatesStrategy = DuplicatesStrategy.INCLUDE
        }

        transform(Log4j2PluginsCacheFileTransformer::class.java)

        // Exclude all the collection types we don't intend to use
        exclude("it/unimi/dsi/fastutil/booleans/**")
        exclude("it/unimi/dsi/fastutil/bytes/**")
        exclude("it/unimi/dsi/fastutil/chars/**")
        exclude("it/unimi/dsi/fastutil/doubles/**")
        exclude("it/unimi/dsi/fastutil/floats/**")
        exclude("it/unimi/dsi/fastutil/longs/**")
        exclude("it/unimi/dsi/fastutil/shorts/**")

        // Exclude the fastutil IO utilities - we don't use them.
        exclude("it/unimi/dsi/fastutil/io/**")

        // Exclude most of the int types - Object2IntMap have a values() method that returns an
        // IntCollection, and we need Int2ObjectMap
        exclude("it/unimi/dsi/fastutil/ints/*Int2Boolean*")
        exclude("it/unimi/dsi/fastutil/ints/*Int2Byte*")
        exclude("it/unimi/dsi/fastutil/ints/*Int2Char*")
        exclude("it/unimi/dsi/fastutil/ints/*Int2Double*")
        exclude("it/unimi/dsi/fastutil/ints/*Int2Float*")
        exclude("it/unimi/dsi/fastutil/ints/*Int2Int*")
        exclude("it/unimi/dsi/fastutil/ints/*Int2Long*")
        exclude("it/unimi/dsi/fastutil/ints/*Int2Short*")
        exclude("it/unimi/dsi/fastutil/ints/*Int2Reference*")
        exclude("it/unimi/dsi/fastutil/ints/IntAVL*")
        exclude("it/unimi/dsi/fastutil/ints/IntArrayF*")
        exclude("it/unimi/dsi/fastutil/ints/IntArrayI*")
        exclude("it/unimi/dsi/fastutil/ints/IntArrayL*")
        exclude("it/unimi/dsi/fastutil/ints/IntArrayP*")
        exclude("it/unimi/dsi/fastutil/ints/IntArraySet*")
        exclude("it/unimi/dsi/fastutil/ints/*IntBi*")
        exclude("it/unimi/dsi/fastutil/ints/Int*Pair")
        exclude("it/unimi/dsi/fastutil/ints/IntLinked*")
        exclude("it/unimi/dsi/fastutil/ints/IntList*")
        exclude("it/unimi/dsi/fastutil/ints/IntHeap*")
        exclude("it/unimi/dsi/fastutil/ints/IntOpen*")
        exclude("it/unimi/dsi/fastutil/ints/IntRB*")
        exclude("it/unimi/dsi/fastutil/ints/IntSorted*")
        exclude("it/unimi/dsi/fastutil/ints/*Priority*")
        exclude("it/unimi/dsi/fastutil/ints/*BigList*")

        // Try to exclude everything BUT Object2Int{LinkedOpen,Open,CustomOpen}HashMap
        exclude("it/unimi/dsi/fastutil/objects/*ObjectArray*")
        exclude("it/unimi/dsi/fastutil/objects/*ObjectAVL*")
        exclude("it/unimi/dsi/fastutil/objects/*Object*Big*")
        exclude("it/unimi/dsi/fastutil/objects/*Object2Boolean*")
        exclude("it/unimi/dsi/fastutil/objects/*Object2Byte*")
        exclude("it/unimi/dsi/fastutil/objects/*Object2Char*")
        exclude("it/unimi/dsi/fastutil/objects/*Object2Double*")
        exclude("it/unimi/dsi/fastutil/objects/*Object2Float*")
        exclude("it/unimi/dsi/fastutil/objects/*Object2IntArray*")
        exclude("it/unimi/dsi/fastutil/objects/*Object2IntAVL*")
        exclude("it/unimi/dsi/fastutil/objects/*Object2IntRB*")
        exclude("it/unimi/dsi/fastutil/objects/*Object2Long*")
        exclude("it/unimi/dsi/fastutil/objects/*Object2Object*")
        exclude("it/unimi/dsi/fastutil/objects/*Object2Reference*")
        exclude("it/unimi/dsi/fastutil/objects/*Object2Short*")
        exclude("it/unimi/dsi/fastutil/objects/*ObjectRB*")
        exclude("it/unimi/dsi/fastutil/objects/*Reference*")

        // Exclude Checker Framework annotations
        exclude("org/checkerframework/checker/**")

        relocate("org.bstats", "com.velocitypowered.proxy.bstats")

        // Include Configurate 3
        val configurateBuildTask = project(":deprecated-configurate3").tasks.named("shadowJar")
        dependsOn(configurateBuildTask)
        from(zipTree(configurateBuildTask.map { it.outputs.files.singleFile }))
    }

    runShadow {
        workingDir = file("run").also(File::mkdirs)
        standardInput = System.`in`
        jvmArgs("-Dvelocity.packet-decode-logging=true")
    }
    named<JavaExec>("run") {
        workingDir = file("run").also(File::mkdirs)
        standardInput = System.`in` // Doesn't work?
    }
}

val projectVersion = version as String
fill {
    project("velocity")

    build {
        channel = BuildChannel.BETA
        versionFamily("3.0.0")
        version(projectVersion)

        downloads {
            register("server:default") {
                file = tasks.shadowJar.flatMap { it.archiveFile }
                nameResolver.set { project, _, version, build -> "$project-$version-$build.jar" }
            }
        }
    }
}

dependencies {
    implementation(project(":velocity-api"))
    implementation(project(":velocity-native"))

    implementation(libs.bundles.log4j)
    implementation(libs.kyori.ansi)
    implementation(libs.netty.codec)
    implementation(libs.netty.codec.haproxy)
    implementation(libs.netty.codec.http)
    implementation(libs.netty.handler)
    implementation(libs.netty.transport.native.epoll)
    implementation(variantOf(libs.netty.transport.native.epoll) { classifier("linux-x86_64") })
    implementation(variantOf(libs.netty.transport.native.epoll) { classifier("linux-aarch_64") })
    implementation(libs.netty.transport.native.iouring)
    implementation(variantOf(libs.netty.transport.native.iouring) { classifier("linux-x86_64") })
    implementation(variantOf(libs.netty.transport.native.iouring) { classifier("linux-aarch_64") })
    implementation(libs.netty.transport.native.kqueue)
    implementation(variantOf(libs.netty.transport.native.kqueue) { classifier("osx-x86_64") })
    implementation(variantOf(libs.netty.transport.native.kqueue) { classifier("osx-aarch_64") })

    implementation(libs.jopt)
    implementation(libs.terminalconsoleappender)
    runtimeOnly(libs.jline)
    runtimeOnly(libs.disruptor)
    implementation(libs.fastutil)
    implementation(platform(libs.adventure.bom))
    implementation(libs.adventure.text.serializer.json.legacy.impl)
    implementation(libs.adventure.facet)
    implementation(libs.completablefutures)
    implementation(libs.nightconfig)
    implementation(libs.bstats)
    implementation(libs.lmbda)
    implementation(libs.asm)
    implementation(libs.bundles.flare)
    compileOnly(libs.spotbugs.annotations)
    compileOnly(libs.auto.service.annotations)
    testImplementation(libs.mockito)

    annotationProcessor(libs.auto.service)
    annotationProcessor(libs.log4j.core)
}
