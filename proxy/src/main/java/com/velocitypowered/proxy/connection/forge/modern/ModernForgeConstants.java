/*
 * Copyright (C) 2018-2023 Velocity Contributors
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

package com.velocitypowered.proxy.connection.forge.modern;

import com.velocitypowered.api.util.GameProfile;
import java.util.function.Function;

/**
 * Constants for use with Modern Forge systems.
 */
public class ModernForgeConstants {
  /**
   * Clients attempting to connect to 1.20.2+ Forge servers will have this token appended to the
   * hostname in the initial handshake packet.
   */
  public static final String MODERN_FORGE_TOKEN = "\0FORGE";

  /**
   * Property used to identify a modern Forge client, used in tandem with "extraData".
   */
  public static final String MODERN_FORGE_CLIENT = "modernForgeClient";
  public static final GameProfile.Property IS_MODERN_FORGE_CLIENT_PROPERTY =
          new GameProfile.Property(MODERN_FORGE_CLIENT, "true", "");

  /**
   * Property used to forward the Forge marker with the Net Version to the backend server.
   */
  public static final String EXTRA_DATA = "extraData";
  public static final Function<String, GameProfile.Property> EXTRA_DATA_PROPERTY =
          (extraData) -> new GameProfile.Property(EXTRA_DATA, extraData, "");
}
