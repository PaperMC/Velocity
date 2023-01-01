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
 * Contains Vanilla {@link BackendConnectionPhase}s.
 *
 * <p>See {@link LegacyForgeHandshakeBackendPhase} for Legacy Forge
 * versions</p>
 */
public final class BackendConnectionPhases {

  /**
   * The backend connection is vanilla.
   */
  public static final BackendConnectionPhase VANILLA = new BackendConnectionPhase() {
  };

  /**
   * The backend connection is unknown at this time.
   */
  public static final BackendConnectionPhase UNKNOWN = new BackendConnectionPhase() {
    @Override
    public boolean consideredComplete() {
      return false;
    }

    @Override
    public boolean handle(VelocityServerConnection serverConn,
        ConnectedPlayer player,
        PluginMessage message) {
      // The connection may be legacy forge. If so, the Forge handler will deal with this
      // for us. Otherwise, we have nothing to do.
      return LegacyForgeHandshakeBackendPhase.NOT_STARTED.handle(serverConn, player, message);
    }
  };

  /**
   * A special backend phase used to indicate that this connection is about to become obsolete
   * (transfer to a new server, for instance) and that Forge messages ought to be forwarded on to an
   * in-flight connection instead.
   */
  public static final BackendConnectionPhase IN_TRANSITION = new BackendConnectionPhase() {
    @Override
    public boolean consideredComplete() {
      return true;
    }
  };

  private BackendConnectionPhases() {
    throw new AssertionError();
  }
}
