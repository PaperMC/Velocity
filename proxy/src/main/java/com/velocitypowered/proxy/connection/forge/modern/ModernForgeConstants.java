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
  public static final String MODERN_FORGE_TOKEN = "FORGE";
  public static final String MODERN_FORGE_CLIENT = "modernForgeClient";
  public static final String EXTRA_DATA = "extraData";

  public static final GameProfile.Property IS_MODERN_FORGE_CLIENT_PROPERTY =
          new GameProfile.Property(MODERN_FORGE_CLIENT, "true", "");

  public static final Function<String, GameProfile.Property> EXTRA_DATA_PROPERTY =
          (extraData) -> new GameProfile.Property(EXTRA_DATA, extraData, "");
}
