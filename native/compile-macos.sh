#!/bin/bash

if [ ! "$CC" ]; then
  export CC=clang
fi

if [ ! -d isa-l ]; then
  echo "Cloning isa-l..."
  git clone https://github.com/intel/isa-l
fi

echo "Compiling isa-l..."
cd isa-l || exit
make -f Makefile.unx
cd ..

CFLAGS="-O2 -I$JAVA_HOME/include/ -I$JAVA_HOME/include/darwin/ -fPIC -shared -Wall -Werror -fomit-frame-pointer"
ARCH=$(uname -m)
mkdir -p src/main/resources/macos_$ARCH
$CC $CFLAGS -Iisa-l/include src/main/c/jni_util.c src/main/c/jni_zlib_deflate.c src/main/c/jni_zlib_inflate.c \
    isa-l/bin/isa-l.a -o src/main/resources/macos_$ARCH/velocity-compress.dylib
$CC $CFLAGS -shared src/main/c/jni_util.c src/main/c/jni_cipher_macos.c \
    -o src/main/resources/macos_$ARCH/velocity-cipher.dylib -lSystem