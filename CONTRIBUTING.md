Contributing to Velocity
==========================
PaperMC is happy you're willing to contribute to our projects. We are usually
very lenient with all submitted PRs, but there are still some guidelines you
can follow to make the approval process go more smoothly.

## Use a Personal Fork and not an Organization

Velocity will routinely modify your PR, whether it's a quick rebase or to take
care of any minor nitpicks we might have. Often, it's better for us to solve
these problems for you than make you go back and forth trying to fix them
yourself.

Unfortunately, if you use an organization for your PR, it prevents Velocity
from modifying it. To avoid this, please do not use repositories on
organizations for PRs.

## Requirements

To get started with making changes, you'll need the following software:

- `git`;
- A Java 21 or later JDK.
    - [Adoptium](https://adoptium.net/) has builds for most operating systems.
    - Velocity requires JDK 25 to build, however, makes use of Gradle's
      [Toolchains](https://docs.gradle.org/current/userguide/toolchains.html)
      feature, so Gradle will automatically provision JDK 25 for compilation if
      it cannot find an existing installation.

It is strongly recommended that you are familiar with the Minecraft protocol,
proficient with using Java, and have familiarity with the libraries used in
Velocity (particularly [Netty](https://netty.io), [Google Guava](https://github.com/google/guava),
and the [Checker Framework annotations](https://checkerframework.org/)).
While you can certainly work with the Velocity codebase without knowing any
of this, it can be risky to proceed.

## Building and Testing

Clone your fork, open it in your IDE of choice and build it as described in
the [README](README.md#building): `./gradlew build` runs the full build cycle,
including Checkstyle and the tests. On Windows, remove the `./` at the
beginning of `gradlew` commands.

To try your changes, run the `-all` JAR from `proxy/build/libs` with your
backend test servers set up to run behind it.

To use your build of the Velocity API in an external plugin, run
`./gradlew publishToMavenLocal` and add `mavenLocal()` as a repository in your
plugin's build. Make sure to remove `mavenLocal()` when you are done testing,
see the [Gradle docs](https://docs.gradle.org/current/userguide/declaring_repositories.html#sec:case-for-maven-local)
for more details.

## PR Policy

We'll accept changes that make sense. You should be able to justify their
existence, along with any maintenance costs that come with them.
Remember that these changes will affect everyone who runs Velocity, not just
you and your network.

While we will fix minor formatting issues, you should stick to the guide below
when making and submitting changes.

### Contribution Ownership and AI Assistance

Tools that generate code or text can be useful while working on a contribution,
but they are not a substitute for the contributor. You are responsible for every
part of what you submit. Only submit changes that you understand, have personally
reviewed, and have verified to the best of your ability. You should be able to
explain why the change is needed, how it works, and what tradeoffs it makes.

Project communication, including issues, discussion posts, pull request
descriptions, comments, and review responses, should be written by you in your
own words. Do not use an AI tool as a proxy for the conversation. Validate any
tool output you rely on, then communicate the relevant conclusions yourself.

Do not conceal or misrepresent meaningful AI involvement. Be transparent about
how these tools contributed to your work; that transparency does not replace
your responsibility for the contribution.

Review is a collaborative process, not a way to outsource completion of a
generated change to maintainers. Be prepared to answer questions about your
reasoning and to remain involved until the contribution is complete. We may
close contributions when the author cannot explain or take responsibility for
the submitted work.

### Branches

Generally, PRs should be targeted at the repository's default branch, where
active development happens. If you're unsure which branch to target, for
example for a change tied to an upcoming Minecraft version, ask in the
[Discord](https://discord.gg/papermc) or open an issue before starting work.

## Formatting

Velocity follows the [Google Code Style](https://google.github.io/styleguide/javaguide.html)
for Java. Velocity will not build if any Checkstyle issues are found, so make
sure that you are properly adhering to the code style.

Every Java file starts with the license header from `HEADER.txt`, and unused
imports are not allowed. Both are enforced by
[Spotless](https://github.com/diffplug/spotless); run `./gradlew spotlessApply`
to fix them automatically.
