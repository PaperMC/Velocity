/*
 * Copyright (C) 2021-2023 Velocity Contributors
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

package com.velocitypowered.proxy.util;

import com.velocitypowered.api.proxy.server.ServerInfoForwardingMode;
import com.velocitypowered.proxy.config.PlayerInfoForwarding;
import com.velocitypowered.proxy.config.VelocityConfiguration;

/**
 * Utilities to convert server info forward mode to Player info forwarding.
 */
public class ServerForwardingModeUtil {

  /**
   * Converts server info forward mode to Player info forwarding.
   *
   * @param configuration velocity config.
   * @param mode server info forwarding mode.
   *
   * @return player info forwarding
   */
  public static PlayerInfoForwarding toPlayerInfoForwarding(VelocityConfiguration configuration, ServerInfoForwardingMode mode) {
    return switch (mode) {
      case FOLLOWUP -> configuration.getPlayerInfoForwardingMode();
      case LEGACY -> PlayerInfoForwarding.LEGACY;
      case MODERN -> PlayerInfoForwarding.MODERN;
      case BUNGEEGUARD -> PlayerInfoForwarding.BUNGEEGUARD;
    };
  }

}
