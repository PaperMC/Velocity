#!/bin/bash

if [ ! -d zlib-cf ]; then
  echo "Cloning Cloudflare zlib..."
  git clone -b gcc.amd64 https://github.com/cloudflare/zlib.git zlib-cf
fi

echo "Compiling Cloudflare zlib..."
cd zlib-cf
CFLAGS="-fPIC -O3 -flto" AR=gcc-ar RANLIB=gcc-ranlib ./configure --static
make clean && make
cd ..

# Modify as you need.
MBEDTLS_ROOT=mbedtls
CFLAGS="-O3 -I$JAVA_HOME/include/ -I$JAVA_HOME/include/linux/ -fPIC -shared -Wl,-z,noexecstack"
gcc $CFLAGS -Izlib-cf src/main/c/jni_util.c src/main/c/jni_zlib_deflate.c src/main/c/jni_zlib_inflate.c \
    src/main/c/jni_zlib_common.c zlib-cf/libz.a -Wl,-z,noexecstack -o src/main/resources/linux_x64/velocity-compress.so
gcc $CFLAGS -I $MBEDTLS_ROOT/include -shared $MBEDTLS_ROOT/library/aes.c $MBEDTLS_ROOT/library/aesni.c \
    $MBEDTLS_ROOT/library/platform.c $MBEDTLS_ROOT/library/platform_util.c src/main/c/jni_util.c src/main/c/jni_cipher.c \
    -o src/main/resources/linux_x64/velocity-cipher.so
