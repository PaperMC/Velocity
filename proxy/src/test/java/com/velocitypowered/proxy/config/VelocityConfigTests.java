package com.velocitypowered.proxy.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.SetEnvironmentVariable;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class VelocityConfigTests {

  private static final Path CONFIG_PATH = Path.of("velocity.toml");
  private static final Path SECRET_PATH = Path.of("forwarding.secret");

  @AfterEach
  void tearDown() throws IOException {
    Files.deleteIfExists(CONFIG_PATH);
    Files.deleteIfExists(SECRET_PATH);
  }

  // Test file creation on first startup

  @Test
  void createsFilesOnFirstStartup() throws IOException {
    final var config = VelocityConfiguration.read(CONFIG_PATH);

    assertTrue(Files.exists(CONFIG_PATH));
    assertTrue(Files.exists(SECRET_PATH));
    assertTrue(config.getForwardingSecret().length > 0);
    assertArrayEquals(Files.readAllBytes(SECRET_PATH), config.getForwardingSecret());
  }

  @Test
  void respectsSecretFileOnFirstStartup() throws IOException {
    Files.writeString(SECRET_PATH, "foo");
    final var config = VelocityConfiguration.read(CONFIG_PATH);

    assertArrayEquals("foo".getBytes(StandardCharsets.UTF_8), config.getForwardingSecret());
  }

  @Test
  @SetEnvironmentVariable(key = "VELOCITY_FORWARDING_SECRET", value = "baz")
  void doesNotCreateSecretFileWhenEnvVarIsSet() throws IOException {
    final var config = VelocityConfiguration.read(CONFIG_PATH);

    assertFalse(Files.exists(SECRET_PATH));
    assertArrayEquals("baz".getBytes(StandardCharsets.UTF_8), config.getForwardingSecret());
  }

  // Test file creation on subsequent startups

  @Test
  void createsSecretFileWhenMissing() throws IOException {
    VelocityConfiguration.read(CONFIG_PATH);
    Files.delete(SECRET_PATH);
    final var config = VelocityConfiguration.read(CONFIG_PATH);

    assertTrue(Files.exists(SECRET_PATH));
    assertTrue(config.getForwardingSecret().length > 0);
    assertArrayEquals(Files.readAllBytes(SECRET_PATH), config.getForwardingSecret());
  }

  @Test
  void recreatesSecretFileWhenEmpty() throws IOException {
    VelocityConfiguration.read(CONFIG_PATH);
    Files.writeString(SECRET_PATH, ""); // truncates existing
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
