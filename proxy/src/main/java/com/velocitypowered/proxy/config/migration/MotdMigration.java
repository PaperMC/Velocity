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

import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.apache.logging.log4j.Logger;

/**
 * Migrates MOTD builtin configuration from legacy or json format to MiniMessage.
 */
public final class MotdMigration implements ConfigurationMigration {
  @Override
  public boolean shouldMigrate(final CommentedFileConfig config) {
    return configVersion(config) < 2.6;
  }

  @Override
  public void migrate(final CommentedFileConfig config, final Logger logger) {
    final String oldMotd = config.getOrElse("motd", "<#09add3>A Velocity Server");
    final String migratedMotd;
    // JSON Format Migration
    if (oldMotd.strip().startsWith("{")) {
      migratedMotd = MiniMessage.miniMessage().serialize(
                      GsonComponentSerializer.gson().deserialize(oldMotd))
              .replace("\\", "");
    } else {
      // Legacy '&' Format Migration
      migratedMotd = MiniMessage.miniMessage().serialize(
              LegacyComponentSerializer.legacyAmpersand().deserialize(oldMotd));
    }

    config.set("motd", migratedMotd);

    config.setComment("motd",
            " What should be the MOTD? This gets displayed when the player adds your server to\n"
                    + " their server list. Only MiniMessage format is accepted.");
    config.set("config-version", "2.6");
  }
}
