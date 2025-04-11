#!/usr/bin/env bash

set -e

# make sure we're in the correct directory - the top-level `native` directory
cd "$(dirname "$0")/.." || exit 1

ARCHS=(x86_64 aarch64)
BASE_DOCKERFILE_VARIANTS=(ubuntu-focal ubuntu-jammy alpine)
COMPRESSION_VARIANTS=(ubuntu-focal alpine)

for variant in "${BASE_DOCKERFILE_VARIANTS[@]}"; do
  docker_platforms=""
  for arch in "${ARCHS[@]}"; do
    docker_platforms="$docker_platforms --platform linux/${arch}"
  done

  echo "Building base build image for $variant..."
  docker build -t velocity-native-build:$variant $docker_platforms -f build-support/$variant.Dockerfile .
done

for arch in "${ARCHS[@]}"; do
  for variant in "${BASE_DOCKERFILE_VARIANTS[@]}"; do
    echo "Building native crypto for $arch on $variant..."

    docker run --rm -v "$(pwd)":/app --platform linux/${arch} velocity-native-build:$variant /bin/bash -c "cd /app && ./build-support/compile-linux-crypto.sh"
  done

  for variant in "${COMPRESSION_VARIANTS[@]}"; do
    echo "Building native compression for $arch on $variant..."
    docker run --rm -v "$(pwd)":/app --platform linux/${arch} velocity-native-build:$variant /bin/bash -c "cd /app && ./build-support/compile-linux-compress.sh"
  done
done