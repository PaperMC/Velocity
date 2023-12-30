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

/**
 * Constants for use with Modern Forge systems.
 */
public class ModernForgeConstants {
  public static final String MODERN_FORGE_TOKEN = "FORGE";
  public static int MODERN_FORGE_NAT_VERSION = 0;

  public static String getModernForgeHostnameToken() {
    return MODERN_FORGE_NAT_VERSION == 0 ? MODERN_FORGE_TOKEN : "\0"
            + MODERN_FORGE_TOKEN + MODERN_FORGE_NAT_VERSION;
  }

  /**
   * Align the acquisition logic with the internal code of Forge.
   *
   * @param hostName address from the client
   */
  public static void initNatVersion(String hostName) {
    int idx = hostName.indexOf('\0');
    if (idx != -1) {
      for (var pt : hostName.split("\0")) {
        if (pt.startsWith(MODERN_FORGE_TOKEN)) {
          if (pt.length() > MODERN_FORGE_TOKEN.length()) {
            MODERN_FORGE_NAT_VERSION = Integer.parseInt(pt.substring(MODERN_FORGE_TOKEN.length()));
          }
        }
      }
    }
  }
}
