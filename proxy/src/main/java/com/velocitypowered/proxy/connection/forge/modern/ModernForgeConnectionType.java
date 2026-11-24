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

import static com.velocitypowered.proxy.connection.forge.modern.ModernForgeConstants.EXTRA_DATA_PROPERTY;
import static com.velocitypowered.proxy.connection.forge.modern.ModernForgeConstants.IS_MODERN_FORGE_CLIENT_PROPERTY;
import static com.velocitypowered.proxy.connection.forge.modern.ModernForgeConstants.MODERN_FORGE_TOKEN;

import com.velocitypowered.api.util.GameProfile;
import com.velocitypowered.proxy.config.PlayerInfoForwarding;
import com.velocitypowered.proxy.connection.backend.BackendConnectionPhases;
import com.velocitypowered.proxy.connection.client.ClientConnectionPhases;
import com.velocitypowered.proxy.connection.util.ConnectionTypeImpl;

/**
 * Contains extra logic to handle Forge 1.20.2+ clients.
 */
public class ModernForgeConnectionType extends ConnectionTypeImpl {

  public final String hostName;

  /**
   * initialize the host name into an internal variable.
   *
   * @param hostName address from the client
   */
  public ModernForgeConnectionType(String hostName) {
    super(ClientConnectionPhases.VANILLA,
        BackendConnectionPhases.VANILLA);
    this.hostName = hostName;
  }

  /**
   * Align the acquisition logic with the internal code of Forge.
   *
   * @return returns the modern Forge token with the Net Version.
   */
  public String getModernToken() {
    int netVersion = 0;
    int idx = hostName.indexOf('\0');
    if (idx != -1) {
      for (var pt : hostName.split("\0")) {
        if (pt.startsWith(MODERN_FORGE_TOKEN)) {
          if (pt.length() > MODERN_FORGE_TOKEN.length()) {
            netVersion = Integer.parseInt(
                    pt.substring(MODERN_FORGE_TOKEN.length()));
            break;
          }
        }
      }
    }
    return MODERN_FORGE_TOKEN + (netVersion == 0 ? "" : netVersion);
  }

  @Override
  public GameProfile addGameProfileTokensIfRequired(GameProfile original,
      PlayerInfoForwarding forwardingType) {
    // We can't forward the FORGE token to the server when we are running in legacy (or bungeeguard) forwarding mode,
    // since both use the "hostname" field in the handshake. We add a special property to the
    // profile instead, which will be ignored by non-Forge servers and can be intercepted by a
    // Forge coremod, such as SpongeForge, BungeeForge, or ProxyCompatibleForge.
    if (forwardingType == PlayerInfoForwarding.LEGACY || forwardingType == PlayerInfoForwarding.BUNGEEGUARD) {
      return original.addProperty(IS_MODERN_FORGE_CLIENT_PROPERTY)
              .addProperty(EXTRA_DATA_PROPERTY.apply(
                      this.getModernToken().replaceAll("\0", "\1")));
    }

    return original;
  }
}
