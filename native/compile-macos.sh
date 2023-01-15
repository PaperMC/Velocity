#!/bin/bash

if [ ! "$CC" ]; then
  export CC=clang
fi

if [ ! -d libdeflate ]; then
  echo "Cloning libdeflate..."
  git clone https://github.com/ebiggers/libdeflate.git
fi

echo "Compiling libdeflate..."
cd libdeflate || exit
cmake -B build && cmake --build build --target libdeflate_static
cd ..

CFLAGS="-O2 -I$JAVA_HOME/include/ -I$JAVA_HOME/include/darwin/ -fPIC -shared -Wall -Werror -fomit-frame-pointer"
ARCH=$(uname -m)
mkdir -p src/main/resources/macos_$ARCH
$CC $CFLAGS -Ilibdeflate src/main/c/jni_util.c src/main/c/jni_zlib_deflate.c src/main/c/jni_zlib_inflate.c \
    libdeflate/build/libdeflate.a -o src/main/resources/macos_$ARCH/velocity-compress.dylib
$CC $CFLAGS -shared src/main/c/jni_util.c src/main/c/jni_cipher_macos.c \
    -o src/main/resources/macos_$ARCH/velocity-cipher.dylib -lSystem