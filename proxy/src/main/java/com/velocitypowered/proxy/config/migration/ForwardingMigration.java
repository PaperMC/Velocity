/*
 * Copyright (C) 2023 Velocity Contributors
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

package com.velocitypowered.proxy.config.migration;

import static com.velocitypowered.proxy.config.VelocityConfiguration.generateRandomString;

import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.apache.logging.log4j.Logger;

/**
 * Migrate old forwarding secret settings to modern version using an external file.
 */
public final class ForwardingMigration implements ConfigurationMigration {
  @Override
  public boolean shouldMigrate(final CommentedFileConfig config) {
    return configVersion(config) < 2.0;
  }

  @Override
  public void migrate(final CommentedFileConfig config, final Logger logger) throws IOException {
    logger.warn("""
            You are currently using a deprecated configuration version.
            The "forwarding-secret"  parameter is a security hazard and was removed in \
            config version 2.0.
            We will migrate your secret to the "forwarding.secret" file.""");
    final String actualSecret = config.get("forwarding-secret");
    final Path path = Path.of(config.getOrElse("forwarding-secret-file", "forwarding.secret"));
    if (Files.exists(path)) {
      final String fileContents = Files.readString(path);
      if (fileContents.isBlank()) {
        Files.writeString(path, actualSecret == null ? generateRandomString(12) : actualSecret);
      }
    } else {
      Files.createFile(path);
      Files.writeString(path, actualSecret == null ? generateRandomString(12) : actualSecret);
    }
    if (actualSecret != null) {
      config.remove("forwarding-secret");
    }
    config.set("forwarding-secret-file", "forwarding.secret");
    config.setComment("forwarding-secret-file", """
                If you are using modern or BungeeGuard IP forwarding, \
                configure a file that contains a unique secret here.
                The file is expected to be UTF-8 encoded and not empty.""");
    config.set("config-version", "2.0");
  }
}
