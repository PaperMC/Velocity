package com.velocitypowered.proxy.config;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

public class VelocityConfigTests {

  // Test file creation
  private static final Path CONFIG_PATH = Path.of("velocity.toml");
  private static final Path SECRET_PATH = Path.of("forwarding.secret");

  @AfterEach
  void tearDown() throws IOException {
    Files.delete(CONFIG_PATH);
    Files.delete(SECRET_PATH);
  }

  @Test
  void createsFilesOnFirstStartup() throws IOException {
    final var config = VelocityConfiguration.read(CONFIG_PATH);

    assertTrue(Files.exists(CONFIG_PATH));
    assertTrue(Files.exists(SECRET_PATH));
    assertArrayEquals(Files.readAllBytes(SECRET_PATH), config.getForwardingSecret());
  }

  @Test
  void createsSecretFileWhenMissing() throws IOException {
    VelocityConfiguration.read(CONFIG_PATH);
    Files.delete(SECRET_PATH);

    final var config = VelocityConfiguration.read(CONFIG_PATH);
    assertTrue(Files.exists(SECRET_PATH));
    assertArrayEquals(Files.readAllBytes(SECRET_PATH), config.getForwardingSecret());
  }

  @Test
  void doesNotCreateSecretFileWhenEnvVarIsSet() {
    // todo: include junit-pioneer or system-stubs
  }

  @Test
  void recreatesSecretFileWhenEmpty() throws IOException {
    VelocityConfiguration.read(CONFIG_PATH);
    Files.writeString(SECRET_PATH, "");

    final var config = VelocityConfiguration.read(CONFIG_PATH);
    assertTrue(config.getForwardingSecret().length > 0);
    assertArrayEquals(Files.readAllBytes(SECRET_PATH), config.getForwardingSecret());
  }

  @Test
  void throwsWhenSecretFileIsInvalid() throws IOException {
    Files.createDirectory(SECRET_PATH);

    assertThrows(RuntimeException.class, () -> VelocityConfiguration.read(CONFIG_PATH));
  }
}
