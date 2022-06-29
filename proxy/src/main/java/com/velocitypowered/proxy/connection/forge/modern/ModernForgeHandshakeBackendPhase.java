/*
 * Copyright (C) 2018 Velocity Contributors
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

import com.velocitypowered.proxy.connection.ConnectionTypes;
import com.velocitypowered.proxy.connection.MinecraftConnection;
import com.velocitypowered.proxy.connection.backend.BackendConnectionPhase;
import com.velocitypowered.proxy.connection.backend.BackendConnectionPhases;
import com.velocitypowered.proxy.connection.backend.VelocityServerConnection;
import com.velocitypowered.proxy.connection.client.ConnectedPlayer;
import com.velocitypowered.proxy.protocol.packet.LoginPluginMessage;

/**
 * Allows for simple tracking of the phase that the Modern
 * Forge handshake is in (server side).
 */
public enum ModernForgeHandshakeBackendPhase implements BackendConnectionPhase {

  /**
   * Indicates that the handshake has not started, used for {@link BackendConnectionPhases#UNKNOWN}.
   */
  NOT_STARTED {
    @Override
    ModernForgeHandshakeBackendPhase nextPhase() {
      return IN_PROGRESS;
    }
  },

  /**
   * Indicates that the handshake is in progress.
   */
  IN_PROGRESS {
    @Override
    public void onLoginSuccess(VelocityServerConnection serverConnection, ConnectedPlayer player) {
      serverConnection.setConnectionPhase(ModernForgeHandshakeBackendPhase.COMPLETE);
      player.setPhase(ModernForgeHandshakeClientPhase.COMPLETE);
    }

    @Override
    void onTransitionToNewPhase(VelocityServerConnection connection) {
      MinecraftConnection mc = connection.getConnection();
      if (mc != null) {
        mc.setType(ConnectionTypes.MODERN_FORGE);
      }
    }
  },

  /**
   * Indicates that the handshake has been completed.
   */
  COMPLETE {
    @Override
    public boolean consideredComplete() {
      return true;
    }
  };

  /**
   * Creates an instance of the {@link ModernForgeHandshakeBackendPhase}.
   */
  ModernForgeHandshakeBackendPhase() {
  }

  @Override
  public final boolean handle(VelocityServerConnection serverConnection,
                        ConnectedPlayer player,
                        LoginPluginMessage message) {
    if (message.getChannel().equals(ModernForgeConstants.LOGIN_WRAPPER_CHANNEL)) {
      // Get the phase and check if we need to start the next phase.
      ModernForgeHandshakeBackendPhase newPhase = getNewPhase(serverConnection, message);

      // Update phase on server
      serverConnection.setConnectionPhase(newPhase);

      // Write the packet to the player, we don't need it now.
      player.getConnection().write(message.retain());
      return true;
    }

    // Not handled, fallback
    return false;
  }

  @Override
  public boolean consideredComplete() {
    return false;
  }

  @Override
  public void onDepartForNewServer(VelocityServerConnection serverConnection,
                                   ConnectedPlayer player) {
    // If the server we are departing is modded, we must always reset the client's handshake.
    player.getPhase().resetConnectionPhase(player);
  }

  /**
   * Performs any specific tasks when moving to a new phase.
   *
   * @param connection The server connection
   */
  void onTransitionToNewPhase(VelocityServerConnection connection) {
  }

  /**
   * Gets the next phase, if any (will return self if we are at the end
   * of the handshake).
   *
   * @return The next phase
   */
  ModernForgeHandshakeBackendPhase nextPhase() {
    return this;
  }

  /**
   * Get the phase to act on, depending on the packet that has been sent.
   *
   * @param serverConnection The server Velocity is connecting to
   * @param packet The packet
   * @return The phase to transition to, which may be the same as before.
   */
  private ModernForgeHandshakeBackendPhase getNewPhase(VelocityServerConnection serverConnection,
                                                       LoginPluginMessage packet) {
    ModernForgeHandshakeBackendPhase phaseToTransitionTo = nextPhase();
    if (phaseToTransitionTo != this) {
      phaseToTransitionTo.onTransitionToNewPhase(serverConnection);
    }

    return phaseToTransitionTo;
  }
}
