#!/usr/bin/env bash

set -e

# make sure we're in the correct directory - the top-level `native` directory
cd "$(dirname "$0")/.." || exit 1

# Use docker by default, falling back to podman. Set CONTAINER_ENGINE to pick one explicitly.
if [ -z "$CONTAINER_ENGINE" ]; then
  if command -v docker > /dev/null 2>&1; then
    CONTAINER_ENGINE=docker
  elif command -v podman > /dev/null 2>&1; then
    CONTAINER_ENGINE=podman
  else
    echo "Neither docker nor podman was found on PATH." >&2
    exit 1
  fi
fi

echo "Using container engine: $CONTAINER_ENGINE"

ARCHS=(x86_64 aarch64)
BASE_DOCKERFILE_VARIANTS=(ubuntu-focal ubuntu-jammy alpine)
COMPRESSION_VARIANTS=(ubuntu-focal alpine)

# Build one image per (variant, arch). Passing several --platform flags to a single tagged build
# does not produce a multi-arch tag - only the last architecture keeps the tag, and the run below
# then sees a platform mismatch, treats the image as missing and tries to pull it from a registry.
for variant in "${BASE_DOCKERFILE_VARIANTS[@]}"; do
  for arch in "${ARCHS[@]}"; do
    echo "Building base build image for $variant on $arch..."
    $CONTAINER_ENGINE build -t velocity-native-build:$variant-$arch --platform linux/${arch} \
      -f build-support/$variant.Dockerfile .
  done
done

for arch in "${ARCHS[@]}"; do
  for variant in "${BASE_DOCKERFILE_VARIANTS[@]}"; do
    echo "Building native crypto for $arch on $variant..."

    $CONTAINER_ENGINE run --rm --pull=never -v "$(pwd)":/app --platform linux/${arch} velocity-native-build:$variant-$arch /bin/bash -c "cd /app && ./build-support/compile-linux-crypto.sh"
  done

  for variant in "${COMPRESSION_VARIANTS[@]}"; do
    echo "Building native compression for $arch on $variant..."
    $CONTAINER_ENGINE run --rm --pull=never -v "$(pwd)":/app --platform linux/${arch} velocity-native-build:$variant-$arch /bin/bash -c "cd /app && ./build-support/compile-linux-compress.sh"
  done
done
