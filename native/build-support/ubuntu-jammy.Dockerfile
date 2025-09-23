# Use the official Eclipse Temurin 17.0.12_7-jdk-jammy image as the base image.
# We compile for Ubuntu Jammy Jellyfish (22.04 LTS) as it supports OpenSSL 3.0.
FROM eclipse-temurin:17.0.12_7-jdk-jammy

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