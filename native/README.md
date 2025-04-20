# velocity-natives

This directory contains native acceleration code for Velocity, along with traditional Java fallbacks.

Compression is based on the `libdeflate` library, which is wire-compatible with zlib but significantly faster.

Encryption is based on OpenSSL for Linux, which is the most widely-used encryption library in the world.
OpenSSL has had several different ABIs over the years, so we provide multiple versions of the native
library. Currently we compile against OpenSSL 1.1.x and OpenSSL 3.x.x. For macOS, we use the built-in
CommonCrypto library.

## Supported Platforms

`velocity-natives` is built for the following platforms:

- Linux x86_64
- Linux aarch64
- macOS aarch64 ("Apple Silicon")

For Linux platforms, we provide two versions of the native library: one built against OpenSSL 1.1.x and one built against OpenSSL 3.x.x.
All native libraries are built on various versions of Ubuntu and Alpine:

- Ubuntu 20.04 for OpenSSL 1.1.x support and for compression
- Ubuntu 22.04 for OpenSSL 3.x.x support
- Alpine 3.18 for OpenSSL 3.x.x support and compression (musl libc users only)

## Building

### On Linux

To build the native libraries, you need to have Docker installed and have it set up to perform [multi-platform builds](https://docs.docker.com/build/building/multi-platform/). Then, run the following command:

```bash
./build-support/build-all-linux-natives.sh
```

This will build the native libraries for both OpenSSL 1.1.x and OpenSSL 3.x.x on both x86_64 and aarch64.

### On macOS

To build the native libraries on macOS, you need to have `cmake` installed. You can install it using Homebrew:

```bash
brew install cmake
```

Then, run the following command:

```bash
./build-support/compile-macos.sh
```

This will build the native libraries for macOS aarch64. x86_64 has not been tested, but it should work.

### On any other operating system?

If your OS of choice is a Unix of some sort, you can use the individual Linux build scripts as a base:

- `build-support/compile-linux-compress.sh`
- `build-support/compile-linux-crypto.sh`

You will need to have the necessary build tools installed (a C/C++ toolchain and `cmake`), and you will need to have OpenSSL installed. You will also need to adjust the script to your needs.

If your OS of choice is Windows, you're on your own. It should be possible, but we don't provide any support for it.