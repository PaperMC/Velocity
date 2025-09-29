#!/bin/bash

if [ ! "$CC" ]; then
  # The libdeflate authors recommend that we build using GCC as it produces "slightly faster binaries":
  # https://github.com/ebiggers/libdeflate#for-unix
  export CC=gcc
fi

if [ ! -d libdeflate ]; then
  echo "Cloning libdeflate..."
  git clone --branch v1.21 --single-branch https://github.com/ebiggers/libdeflate.git
fi

echo "Compiling libdeflate..."
cd libdeflate || exit
rm -rf build && cmake -DCMAKE_POSITION_INDEPENDENT_CODE=ON -B build && cmake --build build --target libdeflate_static
cd ..

# Determine if we are on musl libc or glibc
suffix=""
if ldd --version 2>&1 | grep -q musl; then
  suffix="-musl"
fi

CFLAGS="-O2 -I$JAVA_HOME/include/ -I$JAVA_HOME/include/linux/ -fPIC -shared -Wl,-z,noexecstack -Wall -Werror -fomit-frame-pointer"
ARCH=$(uname -m)
mkdir -p src/main/resources/linux_$ARCH
$CC $CFLAGS -Ilibdeflate src/main/c/jni_util.c src/main/c/jni_zlib_deflate.c src/main/c/jni_zlib_inflate.c \
    libdeflate/build/libdeflate.a -o src/main/resources/linux_$ARCH/velocity-compress$suffix.so