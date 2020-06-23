# velocity-natives

This directory contains native acceleration code for Velocity, along with
traditional Java fallbacks.

## Compression

* **Supported platforms**: Linux x86_64 and aarch64, with Java 11 `ByteBuffer` API support as a fallback.
  Compiled on CentOS 7.
* **Rationale**: Using a native zlib wrapper, we can avoid multiple trips into Java just to copy memory around.

## Encryption

* **Supported platforms**: Linux x86_64 (OpenSSL 1.0.x and OpenSSL 1.1.x) and aarch64 (OpenSSL 1.1.x only)
* **Rationale**: Using a C library for encryption means we can limit memory copies. Prior to Java 7, this was the only
  way to use AES-NI extensions on modern processors, but this is less important since JDK 8 has native support.
* OpenSSL is not included in Velocity. Every distribution provides it now. To deal with ABI incompatibilities,
  the native library (which only calls into OpenSSL and contains no cryptographic code) are available for
  CentOS 7 (OpenSSL 1.0.0-based) and Debian 9 (OpenSSL 1.1.0-based) to provide the widest, most reasonable
  compatibility with most modern distributions.

## OS support

The natives intend to have the widest possible range of compatibility with modern Linux distributions
(defined as those being released in or after 2014).

In theory, these libraries can be compiled for any Unix-like system (in the past, we supported macOS),
but interest in other systems is minimal at best, thus we focus on Linux x86_64 and aarch64 as they
are commonly used platforms.

Alpine Linux support is on a "best-effort" basis only. Using `apk add libc6-compat` may enable native support.