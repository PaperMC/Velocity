# Use the official Eclipse Temurin 17.0.12_7-jdk-focal image as the base image.
# We compile for Ubuntu Focal Fossa (20.04 LTS) as it is still supported until 2025, and the crypto
# native is specific to a given OpenSSL version.
FROM eclipse-temurin:17.0.12_7-jdk-focal

# Install required dependencies
RUN apt-get update && apt-get install -y \
    libssl-dev \
    curl \
    git \
    unzip \
    build-essential \
    cmake \
    openssl \
    && rm -rf /var/lib/apt/lists/* \
    && apt-get clean

# Create a non-root user
RUN useradd -m -s /bin/bash -u 1000 -U user
USER user