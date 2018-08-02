#!/bin/bash

# Modify as you need.
gcc -O3 -I$JAVA_HOME/include/ -I$JAVA_HOME/include/linux/ -shared -lz src/main/c/*.c -o src/main/resources/linux_x64/velocity-compress.so