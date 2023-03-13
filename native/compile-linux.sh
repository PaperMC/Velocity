#!/bin/bash

if [ ! "$CC" ]; then
  export CC=gcc
fi

if [ ! -d isa-l ]; then
  echo "Cloning isa-l..."
  git clone https://github.com/intel/isa-l
fi

echo "Compiling isa-l..."
cd isa-l || exit
./autogen.sh
CFLAGS="-fPIC" ./configure
make
cd ..

CFLAGS="-O2 -I$JAVA_HOME/include/ -I$JAVA_HOME/include/linux/ -fPIC -shared -Wl,-z,noexecstack -Wall -Werror -fomit-frame-pointer"
ARCH=$(uname -m)
mkdir -p src/main/resources/linux_$ARCH
$CC $CFLAGS -Iisa-l/include src/main/c/jni_util.c src/main/c/jni_zlib_deflate.c src/main/c/jni_zlib_inflate.c \
    isa-l/.libs/libisal.a -o src/main/resources/linux_$ARCH/velocity-compress.so
$CC $CFLAGS -shared src/main/c/jni_util.c src/main/c/jni_cipher_openssl.c \
    -o src/main/resources/linux_$ARCH/velocity-cipher.so -lcrypto