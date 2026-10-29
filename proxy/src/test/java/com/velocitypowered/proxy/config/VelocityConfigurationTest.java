/*
 * Copyright (C) 2026 Velocity Contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.velocitypowered.proxy.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class VelocityConfigurationTest {

  @TempDir
  Path directory;

  @Test
  void rejectsNewerConfigBeforeReadingLegacyPingPassthrough() throws IOException {
    Path config = writeConfig("3.0", "ping-passthrough = \"all\"\n");
    final String original = Files.readString(config);

    IOException exception = assertThrows(IOException.class, () -> VelocityConfiguration.read(config));

    assertTrue(exception.getMessage().contains("3.0"));
    assertTrue(exception.getMessage().contains("2.9"));
    assertTrue(exception.getMessage().contains("config-version"));
    assertEquals(original, Files.readString(config));
    assertFalse(Files.exists(directory.resolve("forwarding.secret")));
  }

  @Test
  void readsCurrentConfig() throws IOException {
    Path config = writeConfig("2.9", "");

    VelocityConfiguration result = VelocityConfiguration.read(config);

    assertEquals(PingPassthroughMode.DEFAULT, result.getPingPassthrough());
  }

  @ParameterizedTest
  @ValueSource(strings = {"invalid", "NaN", "Infinity"})
  void rejectsInvalidConfigVersion(String version) throws IOException {
    Path config = writeConfig(version, "");
    final String original = Files.readString(config);

    IOException exception = assertThrows(IOException.class, () -> VelocityConfiguration.read(config));

    assertTrue(exception.getMessage().contains("Invalid config-version"));
    assertEquals(original, Files.readString(config));
    assertFalse(Files.exists(directory.resolve("forwarding.secret")));
  }

  @Test
  void stillMigratesOlderPingPassthroughConfig() throws IOException {
    Path config = writeConfig("2.8", "ping-passthrough = \"all\"\n");

    VelocityConfiguration result = VelocityConfiguration.read(config);

    assertEquals(new PingPassthroughMode(true, true, true, true, true),
        result.getPingPassthrough());
    assertTrue(Files.readString(config).contains("config-version = \"2.9\""));
  }

  private Path writeConfig(String version, String extra) throws IOException {
    Path config = directory.resolve("velocity.toml");
    Files.writeString(config, "config-version = \"" + version + "\"\n"
        + "forwarding-secret-file = \"" + directory.resolve("forwarding.secret") + "\"\n"
        + extra);
    return config;
  }
}
