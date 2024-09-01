#!/bin/bash

openssl_version=$(openssl version | awk '{print $2}')

# Extract the major and minor version numbers
major_version=$(echo "$openssl_version" | cut -d. -f1)
minor_version=$(echo "$openssl_version" | cut -d. -f2)

# Determine the appropriate file name based on the version
if [ "$major_version" -eq 1 ] && [ "$minor_version" -eq 1 ]; then
    filename="velocity-cipher-ossl11x.so"
elif [ "$major_version" -eq 3 ]; then
    filename="velocity-cipher-ossl30x.so"
else
    echo "Unsupported OpenSSL version: $openssl_version"
    exit 1
fi

if [ ! "$CC" ]; then
  export CC=gcc
fi

output_file="velocity-cipher.so"
if [ -n "$OPENSSL_VERSION" ]; then
  output_file="velocity-cipher-ossl${OPENSSL_VERSION}.so"
fi

CFLAGS="-O2 -I$JAVA_HOME/include/ -I$JAVA_HOME/include/linux/ -fPIC -shared -Wl,-z,noexecstack -Wall -Werror -fomit-frame-pointer"
ARCH=$(uname -m)
mkdir -p src/main/resources/linux_$ARCH
$CC $CFLAGS -shared src/main/c/jni_util.c src/main/c/jni_cipher_openssl.c \
    -o src/main/resources/linux_$ARCH/$filename -lcrypto