# velocity-natives

This directory contains native acceleration code for Velocity, along with
traditional Java fallbacks.

## Compression

* **Supported platforms**: macOS 10.13, Linux amd64 (precompiled binary is built on Debian 9 with JDK 8)
* **Rationale**: Using a native zlib wrapper, we can avoid multiple trips into Java just to copy memory around.

## Encryption

* **Supported platforms**: macOS 10.13, Linux amd64
* **Rationale**: Using a C library for encryption means we can limit memory copies. Prior to Java 7, this was the only
  way to use AES-NI extensions on modern processors, but this is less important since JDK 8 has native support.
* **Note**: Due to U.S. restrictions on cryptography export, this native is provided in source code form only for now.

## OS support

If you are on Alpine Linux, `apk add libc6-compat` will enable native support.