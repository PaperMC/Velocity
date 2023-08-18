#!/bin/bash

if [ ! "$CC" ]; then
  # The libdeflate authors recommend that we build using GCC as it produces "slightly faster binaries":
  # https://github.com/ebiggers/libdeflate#for-unix
  export CC=gcc
fi

if [ ! -d libdeflate ]; then
  echo "Cloning libdeflate..."
  git clone https://github.com/ebiggers/libdeflate.git
fi

echo "Compiling libdeflate..."
cd libdeflate || exit
cmake -B build && cmake --build build --target libdeflate_static
cd ..

CFLAGS="-O2 -I$JAVA_HOME/include/ -I$JAVA_HOME/include/linux/ -fPIC -shared -Wl,-z,noexecstack -Wall -Werror -fomit-frame-pointer"
ARCH=$(uname -m)
mkdir -p src/main/resources/linux_$ARCH
$CC $CFLAGS -Ilibdeflate src/main/c/jni_util.c src/main/c/jni_zlib_deflate.c src/main/c/jni_zlib_inflate.c \
    libdeflate/build/libdeflate.a -o src/main/resources/linux_$ARCH/velocity-compress.so
$CC $CFLAGS -shared src/main/c/jni_util.c src/main/c/jni_cipher_openssl.c \
    -o src/main/resources/linux_$ARCH/velocity-cipher.so -lcrypto