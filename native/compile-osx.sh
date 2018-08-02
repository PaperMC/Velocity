#!/bin/bash

# Modify as you need.
export JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk1.8.0_144.jdk/Contents/Home
clang -I$JAVA_HOME/include/ -I$JAVA_HOME/include/darwin/ -shared -lz src/main/c/*.c -o src/main/resources/macosx/velocity-compress.dylib