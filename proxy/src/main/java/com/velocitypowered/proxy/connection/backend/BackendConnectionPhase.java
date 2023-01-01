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

package com.velocitypowered.proxy.connection.backend;

import com.velocitypowered.proxy.connection.client.ConnectedPlayer;
import com.velocitypowered.proxy.connection.forge.legacy.LegacyForgeHandshakeBackendPhase;
import com.velocitypowered.proxy.protocol.packet.PluginMessage;

/**
 * Provides connection phase specific actions.
 *
 * <p>Note that Forge phases are found in the enum
 * {@link LegacyForgeHandshakeBackendPhase}.</p>
 */
public interface BackendConnectionPhase {

  /**
   * Handle a plugin message in the context of this phase.
   *
   * @param server the server connection
   * @param player the player
   * @param message The message to handle
   * @return true if handled, false otherwise.
   */
  default boolean handle(VelocityServerConnection server,
      ConnectedPlayer player,
      PluginMessage message) {
    return false;
  }

  /**
   * Indicates whether the connection is considered complete.
   *
   * @return true if so
   */
  default boolean consideredComplete() {
    return true;
  }

  /**
   * Fired when the provided server connection is about to be terminated because the provided player
   * is connecting to a new server.
   *
   * @param serverConnection The server the player is disconnecting from
   * @param player           The player
   */
  default void onDepartForNewServer(VelocityServerConnection serverConnection,
      ConnectedPlayer player) {
  }
}
