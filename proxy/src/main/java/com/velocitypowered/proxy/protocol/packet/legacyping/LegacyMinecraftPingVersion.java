/*
 * Copyright (C) 2019-2021 Velocity Contributors
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

package com.velocitypowered.proxy.protocol.packet.legacyping;

/**
 * The {@code LegacyMinecraftPingVersion} enum represents the various protocol versions
 * used by older Minecraft clients during the server ping process.
 *
 * <p>This enum is used to distinguish between the different legacy versions of Minecraft
 * that have unique ping formats, ensuring compatibility with those older clients.</p>
 *
 * <p>Each constant in this enum corresponds to a specific version of Minecraft that
 * requires a legacy server ping format.</p>
 */
public enum LegacyMinecraftPingVersion {
  MINECRAFT_1_3,
  MINECRAFT_1_4,
  MINECRAFT_1_6
}
