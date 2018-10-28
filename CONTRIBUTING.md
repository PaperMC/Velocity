Thanks for taking the time to submit a contribution to Velocity! Your support
is greatly appreciated.

In this document, we'll give you some tips on making it more likely your
contribution will be pulled.

# Setting up a development environment

This isn't as difficult as you may be led to believe. All you need to do is
clone the Velocity repository in your favorite IDE and have your backend test
servers set up to run behind Velocity.

# Actually working on the code

It is strongly recommended that you are familiar with the Minecraft protocol,
proficient with using Java, and have familiarity with the libraries used in
Velocity (particularly [Netty](https://netty.io), [Google Guava](https://github.com/google/guava),
and the [Checker Framework annotations](https://checkerframework.org/)).
While you can certainly work with the Velocity codebase without knowing any
of this, it can be risky to proceed.

Velocity does not currently obey any one general code style at the moment.
Plans are [in the works](https://github.com/VelocityPowered/Velocity/issues/125)
to define the code style the project will follow.

# Notes on the build

To reduce bugs and ensure code quality, we run the following tools on all commits
and pull requests:

* [Checker Framework](https://checkerframework.org/): an enhancement to Java's type
  system that is designed to help catch bugs. Velocity runs the _Nullness Checker_
  and the _Optional Checker_.
* [Checkstyle](http://checkstyle.sourceforge.net/) (not currently in use): ensures
  that your code is correctly formatted.