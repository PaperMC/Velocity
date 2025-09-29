# Velocity

[![Build Status](https://img.shields.io/github/actions/workflow/status/PaperMC/Velocity/gradle.yml)](https://papermc.io/downloads/velocity)
[![Join our Discord](https://img.shields.io/discord/289587909051416579.svg?logo=discord&label=)](https://discord.gg/papermc)

A Minecraft server proxy with unparalleled server support, scalability,
and flexibility.

Velocity is licensed under the GPLv3 license.

## Goals

* A codebase that is easy to dive into and consistently follows best practices
  for Java projects as much as reasonably possible.
* High performance: handle thousands of players on one proxy.
* A new, refreshing API built from the ground up to be flexible and powerful
  whilst avoiding design mistakes and suboptimal designs from other proxies.
* First-class support for Paper, Sponge, Fabric and Forge. (Other implementations
  may work, but we make every endeavor to support these server implementations
  specifically.)
  
## Building

Velocity is built with [Gradle](https://gradle.org). We recommend using the
wrapper script (`./gradlew`) as our CI builds using it.

It is sufficient to run `./gradlew build` to run the full build cycle.

## Running

Once you've built Velocity, you can copy and run the `-all` JAR from
`proxy/build/libs`. Velocity will generate a default configuration file
and you can configure it from there.

Alternatively, you can get the proxy JAR from the [downloads](https://papermc.io/downloads/velocity)
page.
