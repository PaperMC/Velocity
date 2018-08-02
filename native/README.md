# velocity-natives

This directory contains native acceleration code for Velocity, along with
traditional Java fallbacks.

## Compression

* **Supported platforms**: macOS 10.13, Linux amd64 (precompiled binary is built on Debian 9 with JDK 8)
* **Rationale**: Using a native zlib wrapper, we can avoid multiple trips into Java just to copy memory around.

## Encryption

* No natives available yet, this will use the support inside your Java install.

## OS support

If you are on Alpine Linux, `apk add libc6-compat` will enable native support.