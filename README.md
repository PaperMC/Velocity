# Velocity

[![Build Status](https://img.shields.io/jenkins/s/https/ci.velocitypowered.com/job/velocity/job/master.svg)](https://ci.velocitypowered.com/job/velocity/job/master/)
[![Join our Discord](https://img.shields.io/discord/472484458856185878.svg?logo=discord&label=)](https://discord.gg/8cB9Bgf)

Velocity is a next-generation Minecraft: Java Edition proxy suite. It is
designed specifically for enhanced server support and scalability whilst
not compromising flexibility.

Velocity is licensed under the MIT license for ultimate permissiveness
and expanding the pool of potential contributors and users.

## Goals

* A codebase that is easy to dive into and consistently follows best practices
  for Java projects as much as reasonably possible.
* High performance: handle thousands of players on one proxy.
* A new, refreshing API built from the ground up to be flexible and powerful
  whilst avoiding design mistakes and suboptimal designs from other proxies.
* First-class support for Paper, Sponge, and Forge. (Other implementations
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

Alternatively, you can get the proxy JAR from the [downloads](https://www.velocitypowered.com/downloads)
page.

## Status

Velocity is currently in an alpha state: it is prone to change at any time and
is not currently suitable for production usage. For development and testing
purposes, however, Velocity is fully-fledged and ready to go.

Velocity supports Minecraft 1.8-1.13.1, and has full support for Paper and Sponge.
Forge support is currently not implemented, but Velocity will work with Forge's
vanilla fallback mode.