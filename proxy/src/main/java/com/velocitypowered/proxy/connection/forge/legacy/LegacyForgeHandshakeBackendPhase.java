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

package com.velocitypowered.proxy.connection.forge.legacy;

import com.velocitypowered.proxy.connection.ConnectionTypes;
import com.velocitypowered.proxy.connection.MinecraftConnection;
import com.velocitypowered.proxy.connection.backend.BackendConnectionPhase;
import com.velocitypowered.proxy.connection.backend.BackendConnectionPhases;
import com.velocitypowered.proxy.connection.backend.VelocityServerConnection;
import com.velocitypowered.proxy.connection.client.ConnectedPlayer;
import com.velocitypowered.proxy.protocol.packet.PluginMessage;
import javax.annotation.Nullable;

/**
 * Allows for simple tracking of the phase that the Legacy Forge handshake is in (server side).
 */
public enum LegacyForgeHandshakeBackendPhase implements BackendConnectionPhase {

  /**
   * Indicates that the handshake has not started, used for
   * {@link BackendConnectionPhases#UNKNOWN}.
   */
  NOT_STARTED(LegacyForgeConstants.SERVER_HELLO_DISCRIMINATOR) {
    @Override
    LegacyForgeHandshakeBackendPhase nextPhase() {
      return HELLO;
    }
  },

  /**
   * Sent a hello to the client, waiting for a hello back before sending the mod list.
   */
  HELLO(LegacyForgeConstants.MOD_LIST_DISCRIMINATOR) {
    @Override
    LegacyForgeHandshakeBackendPhase nextPhase() {
      return SENT_MOD_LIST;
    }

    @Override
    void onTransitionToNewPhase(VelocityServerConnection connection) {
      // We must always reset the handshake before a modded connection is established if
      // we haven't done so already.
      MinecraftConnection mc = connection.getConnection();
      if (mc != null) {
        mc.setType(ConnectionTypes.LEGACY_FORGE);
      }
      connection.getPlayer().sendLegacyForgeHandshakeResetPacket();
    }
  },

  /**
   * The mod list from the client has been accepted and a server mod list has been sent. Waiting for
   * the client to acknowledge.
   */
  SENT_MOD_LIST(LegacyForgeConstants.REGISTRY_DISCRIMINATOR) {
    @Override
    LegacyForgeHandshakeBackendPhase nextPhase() {
      return SENT_SERVER_DATA;
    }
  },

  /**
   * The server data is being sent or has been sent, and is waiting for the client to acknowledge it
   * has processed this.
   */
  SENT_SERVER_DATA(LegacyForgeConstants.ACK_DISCRIMINATOR) {
    @Override
    LegacyForgeHandshakeBackendPhase nextPhase() {
      return WAITING_ACK;
    }
  },

  /**
   * Waiting for the client to acknowledge before completing handshake.
   */
  WAITING_ACK(LegacyForgeConstants.ACK_DISCRIMINATOR) {
    @Override
    LegacyForgeHandshakeBackendPhase nextPhase() {
      return COMPLETE;
    }
  },

  /**
   * The server has completed the handshake and will continue after the client ACK.
   */
  COMPLETE(null) {
    @Override
    public boolean consideredComplete() {
      return true;
    }
  };

  @Nullable
  private final Integer packetToAdvanceOn;

  /**
   * Creates an instance of the {@link LegacyForgeHandshakeBackendPhase}.
   *
   * @param packetToAdvanceOn The ID of the packet discriminator that indicates that the server has
   *                          moved onto a new phase, and as such, Velocity should do so too
   *                          (inspecting {@link #nextPhase()}. A null indicates there is no further
   *                          phase to transition to.
   */
  LegacyForgeHandshakeBackendPhase(@Nullable Integer packetToAdvanceOn) {
    this.packetToAdvanceOn = packetToAdvanceOn;
  }

  @Override
  public final boolean handle(VelocityServerConnection serverConnection,
      ConnectedPlayer player,
      PluginMessage message) {
    if (message.getChannel().equals(LegacyForgeConstants.FORGE_LEGACY_HANDSHAKE_CHANNEL)) {
      // Get the phase and check if we need to start the next phase.
      LegacyForgeHandshakeBackendPhase newPhase = getNewPhase(serverConnection, message);

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
   * Gets the next phase, if any (will return self if we are at the end of the handshake).
   *
   * @return The next phase
   */
  LegacyForgeHandshakeBackendPhase nextPhase() {
    return this;
  }

  /**
   * Get the phase to act on, depending on the packet that has been sent.
   *
   * @param serverConnection The server Velocity is connecting to
   * @param packet           The packet
   * @return The phase to transition to, which may be the same as before.
   */
  private LegacyForgeHandshakeBackendPhase getNewPhase(VelocityServerConnection serverConnection,
      PluginMessage packet) {
    if (packetToAdvanceOn != null
        && LegacyForgeUtil.getHandshakePacketDiscriminator(packet) == packetToAdvanceOn) {
      LegacyForgeHandshakeBackendPhase phaseToTransitionTo = nextPhase();
      phaseToTransitionTo.onTransitionToNewPhase(serverConnection);
      return phaseToTransitionTo;
    }

    return this;
  }
}
