#!/bin/bash

# Modify as you need.
MBEDTLS_ROOT=mbedtls
export JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk1.8.0_144.jdk/Contents/Home
CFLAGS="-O3 -I$JAVA_HOME/include/ -I$JAVA_HOME/include/darwin/ -fPIC -shared"

clang $CFLAGS -lz src/main/c/jni_util.c src/main/c/jni_zlib_deflate.c src/main/c/jni_zlib_inflate.c \
    src/main/c/jni_zlib_common.c -o src/main/resources/macosx/velocity-compress.dylib
clang $CFLAGS -I $MBEDTLS_ROOT/include -shared $MBEDTLS_ROOT/library/aes.c $MBEDTLS_ROOT/library/aesni.c \
    $MBEDTLS_ROOT/library/platform.c $MBEDTLS_ROOT/library/platform_util.c src/main/c/jni_util.c src/main/c/jni_cipher.c \
    -o src/main/resources/macosx/velocity-cipher.dylib