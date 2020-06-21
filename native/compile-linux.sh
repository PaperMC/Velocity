#!/bin/bash

if [ ! -d libdeflate ]; then
  echo "Cloning libdeflate..."
  git clone https://github.com/ebiggers/libdeflate.git
fi

echo "Compiling libdeflate..."
cd libdeflate || exit
make
cd ..

# Modify as you need.
MBEDTLS_ROOT=mbedtls
CFLAGS="-O3 -I$JAVA_HOME/include/ -I$JAVA_HOME/include/linux/ -fPIC -shared -Wl,-z,noexecstack"
ARCH=$(uname -m)
mkdir -p src/main/resources/linux_$ARCH
gcc $CFLAGS -Ilibdeflate src/main/c/jni_util.c src/main/c/jni_zlib_deflate.c src/main/c/jni_zlib_inflate.c \
    libdeflate/libdeflate.a -o src/main/resources/linux_$ARCH/velocity-compress.so
gcc $CFLAGS -I $MBEDTLS_ROOT/include -shared $MBEDTLS_ROOT/library/aes.c $MBEDTLS_ROOT/library/aesni.c \
    $MBEDTLS_ROOT/library/platform.c $MBEDTLS_ROOT/library/platform_util.c src/main/c/jni_util.c src/main/c/jni_cipher.c \
    -o src/main/resources/linux_$ARCH/velocity-cipher.so