# Velocity-CTD

[![Join my Discord](https://media3.giphy.com/media/v1.Y2lkPTc5MGI3NjExdG5sdGgwazRwYjh4djdsdXJwcHR5ajZrNGE2NDBvcTUzdXltbHp1cCZlcD12MV9pbnRlcm5hbF9naWZfYnlfaWQmY3Q9Zw/fGIwpaCrtkFdHVksSu/giphy.gif)](https://discord.gg/beer)

A Minecraft server proxy with unparalleled server support, scalability,
and flexibility.

Velocity-CTD is licensed under the GPLv3 license.

## Goals

* A codebase that is easy to dive into and consistently follows best practices
  for Java projects as much as reasonably possible.
* High performance: handle thousands of players on one proxy.
* A new, refreshing API built from the ground up to be flexible and powerful
  whilst avoiding design mistakes and suboptimal designs from other proxies.
* First-class support for Paper, Sponge, Fabric and Forge. (Other implementations
  may work, but we make every endeavor to support these server implementations
  specifically.)
* Features that deliver an "all-in-one" experience with various features that
  we believe every network wants and needs.

## Additional Features/Removals

* Configurable `/alert` command sends messages across your entire network.
* Configurable `/find` command that locates yourself and other users.
* Configurable `/ping` command that displays your and other users' ping.
* The `/send` supports sending users from `{SERVER_FROM}` to `{SERVER_TO}`.
* Configurable `/showall` command that displays all users connected to a specific
  instance rather than flooding your chat with users connected everywhere.
* Fallback servers allow users to be sent to the least populated server,
  which will cycle for even distribution.
* Configurable server brand and server pinger message.
* Preliminary MiniMessage support to permit full configurability of all Velocity
  messages.
* Removal of all language files except `messages.properties` to preserve
  maintainability. PR's are welcome that reimplement all language files
  with our changes.

## Building

Velocity is built with [Gradle](https://gradle.org). We recommend using the
wrapper script (`./gradlew`) as our CI builds using it.

It is sufficient to run `./gradlew build` to run the full build cycle.

We do not explicitly provide publicly available builds. Following the steps
above ensures that you receive the latest iterations of our software.

## Running

Once you've built Velocity, you can copy and run the `-all` JAR from
`proxy/build/libs`. Velocity will generate a default configuration file
and you can configure it from there.
