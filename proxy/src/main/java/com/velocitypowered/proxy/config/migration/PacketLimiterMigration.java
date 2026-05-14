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

package com.velocitypowered.proxy.config.migration;

import static com.velocitypowered.proxy.config.VelocityConfiguration.PacketLimiterConfig.DEFAULT;

import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import org.apache.logging.log4j.Logger;

/**
 * Configuration migration for the new [packet-limiter] section.
 * Config version 2.7 may contain this section with only the `interval`, `packets-per-second`
 * and `bytes-per-second` attributes. Config version 2.8 enforces these exist, adds the new
 * `decompressed-bytes-per-second` attribute, adjusts the new default, and adds comments.
 */
public final class PacketLimiterMigration implements ConfigurationMigration {

  @Override
  public boolean shouldMigrate(CommentedFileConfig config) {
    return configVersion(config) < 2.8;
  }

  @Override
  public void migrate(CommentedFileConfig config, Logger logger) {
    config.set("packet-limiter.interval", DEFAULT.interval());
    config.set("packet-limiter.packets-per-second", DEFAULT.pps());
    config.set("packet-limiter.bytes-per-second", DEFAULT.bytes());
    config.set("packet-limiter.decompressed-bytes-per-second", DEFAULT.bytesAfterDecompression());

    config.setComment("packet-limiter.interval", """
        Size of the moving time window in seconds used to calculate average rates.
        A larger window tolerates short bursts while still enforcing the configured limits over time.""");

    config.setComment("packet-limiter.packets-per-second", """
        Maximum average number of packets per second a client may send. -1 disables this check.""");

    config.setComment("packet-limiter.bytes-per-second", """
        Maximum average number of compressed (on-wire) bytes per second a client may send. -1 disables this check.""");

    config.setComment("packet-limiter.decompressed-bytes-per-second", """
        Maximum average number of decompressed bytes per second a client may send.
        Protects against compression bomb attacks where small packets expand to excessive sizes after decompression.
        -1 disables this check.""");

    config.set("config-version", "2.8");
  }
}
