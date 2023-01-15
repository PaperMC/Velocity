import com.github.jengelman.gradle.plugins.shadow.transformers.Log4j2PluginsCacheFileTransformer

plugins {
    application
    `set-manifest-impl-version`
    id("com.github.johnrengelman.shadow") version "7.1.0"
}

application {
    mainClass.set("com.velocitypowered.proxy.Velocity")
}

tasks {
    withType<Checkstyle> {
        exclude("**/com/velocitypowered/proxy/protocol/packet/**")
    }

    jar {
        manifest {
            attributes["Implementation-Title"] = "Velocity"
            attributes["Implementation-Vendor"] = "Velocity Contributors"
            attributes["Multi-Release"] = "true"
        }
    }

    shadowJar {
        transform(Log4j2PluginsCacheFileTransformer::class.java)

        // Exclude all the collection types we don"t intend to use
        exclude("it/unimi/dsi/fastutil/booleans/**")
        exclude("it/unimi/dsi/fastutil/bytes/**")
        exclude("it/unimi/dsi/fastutil/chars/**")
        exclude("it/unimi/dsi/fastutil/doubles/**")
        exclude("it/unimi/dsi/fastutil/floats/**")
        exclude("it/unimi/dsi/fastutil/longs/**")
        exclude("it/unimi/dsi/fastutil/shorts/**")

        // Exclude the fastutil IO utilities - we don"t use them.
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
        exclude("it/unimi/dsi/fastutil/ints/IntArray*")
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
    }
}

val adventureVersion: String by project.extra
val adventureFacetVersion: String by project.extra
val asyncHttpClientVersion: String by project.extra
val bstatsVersion: String by project.extra
val caffeineVersion: String by project.extra
val completableFuturesVersion: String by project.extra
val disruptorVersion: String by project.extra
val fastutilVersion: String by project.extra
val flareVersion: String by project.extra
val jansiVersion: String by project.extra
val joptSimpleVersion: String by project.extra
val lmbdaVersion: String by project.extra
val log4jVersion: String by project.extra
val nettyVersion: String by project.extra
val nightConfigVersion: String by project.extra
val semver4jVersion: String by project.extra
val terminalConsoleAppenderVersion: String by project.extra

dependencies {
    implementation(project(":velocity-api"))
    implementation(project(":velocity-native"))

    implementation("io.netty:netty-codec:${nettyVersion}")
    implementation("io.netty:netty-codec-haproxy:${nettyVersion}")
    implementation("io.netty:netty-codec-http:${nettyVersion}")
    implementation("io.netty:netty-handler:${nettyVersion}")
    implementation("io.netty:netty-transport-native-epoll:${nettyVersion}")
    implementation("io.netty:netty-transport-native-epoll:${nettyVersion}:linux-x86_64")
    implementation("io.netty:netty-transport-native-epoll:${nettyVersion}:linux-aarch_64")

    implementation("org.apache.logging.log4j:log4j-api:${log4jVersion}")
    implementation("org.apache.logging.log4j:log4j-core:${log4jVersion}")
    implementation("org.apache.logging.log4j:log4j-slf4j-impl:${log4jVersion}")
    implementation("org.apache.logging.log4j:log4j-iostreams:${log4jVersion}")
    implementation("org.apache.logging.log4j:log4j-jul:${log4jVersion}")

    implementation("net.sf.jopt-simple:jopt-simple:$joptSimpleVersion") // command-line options
    implementation("net.minecrell:terminalconsoleappender:$terminalConsoleAppenderVersion")
    runtimeOnly("org.jline:jline-terminal-jansi:$jansiVersion")  // Needed for JLine
    runtimeOnly("com.lmax:disruptor:$disruptorVersion") // Async loggers

    implementation("it.unimi.dsi:fastutil-core:$fastutilVersion")

    implementation(platform("net.kyori:adventure-bom:$adventureVersion"))
    implementation("net.kyori:adventure-nbt")
    implementation("net.kyori:adventure-platform-facet:$adventureFacetVersion")

    implementation("org.asynchttpclient:async-http-client:$asyncHttpClientVersion")

    implementation("com.spotify:completable-futures:$completableFuturesVersion")

    implementation("com.electronwill.night-config:toml:$nightConfigVersion")

    implementation("org.bstats:bstats-base:$bstatsVersion")
    implementation("org.lanternpowered:lmbda:$lmbdaVersion")

    implementation("com.github.ben-manes.caffeine:caffeine:$caffeineVersion")

    implementation("space.vectrix.flare:flare:$flareVersion")
    implementation("space.vectrix.flare:flare-fastutil:$flareVersion")

    compileOnly("com.github.spotbugs:spotbugs-annotations:4.7.3")

    testImplementation("org.mockito:mockito-core:3.+")
}
