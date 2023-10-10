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

import com.velocitypowered.api.util.ModInfo;
import com.velocitypowered.proxy.connection.MinecraftConnection;
import com.velocitypowered.proxy.connection.MinecraftSessionHandler;
import com.velocitypowered.proxy.connection.backend.VelocityServerConnection;
import com.velocitypowered.proxy.connection.client.ClientConnectionPhase;
import com.velocitypowered.proxy.connection.client.ClientPlaySessionHandler;
import com.velocitypowered.proxy.connection.client.ConnectedPlayer;
import com.velocitypowered.proxy.protocol.packet.PluginMessage;
import java.util.List;
import javax.annotation.Nullable;

/**
 * Allows for simple tracking of the phase that the Legacy Forge handshake is in.
 */
public enum LegacyForgeHandshakeClientPhase implements ClientConnectionPhase {

  /**
   * No handshake packets have yet been sent. Transition to {@link #HELLO} when the ClientHello is
   * sent.
   */
  NOT_STARTED(LegacyForgeConstants.CLIENT_HELLO_DISCRIMINATOR) {
    @Override
    LegacyForgeHandshakeClientPhase nextPhase() {
      return HELLO;
    }

    @Override
    public void onFirstJoin(ConnectedPlayer player) {
      // We have something special to do for legacy Forge servers - during first connection the FML
      // handshake will getNewPhase to complete regardless. Thus, we need to ensure that a reset
      // packet is ALWAYS sent on first switch.
      //
      // As we know that calling this branch only happens on first join, we set that if we are a
      // Forge client that we must reset on the next switch.
      player.setPhase(LegacyForgeHandshakeClientPhase.COMPLETE);
    }

    @Override
    boolean onHandle(ConnectedPlayer player,
        PluginMessage message,
        MinecraftConnection backendConn) {
      // If we stay in this phase, we do nothing because it means the packet wasn't handled.
      // Returning false indicates this
      return false;
    }
  },

  /**
   * Client and Server exchange pleasantries. Transition to {@link #MOD_LIST} when the ModList is
   * sent.
   */
  HELLO(LegacyForgeConstants.MOD_LIST_DISCRIMINATOR) {
    @Override
    LegacyForgeHandshakeClientPhase nextPhase() {
      return MOD_LIST;
    }
  },

  /**
   * The Mod list is sent to the server, captured by Velocity. Transition to {@link
   * #WAITING_SERVER_DATA} when an ACK is sent, which indicates to the server to start sending state
   * data.
   */
  MOD_LIST(LegacyForgeConstants.ACK_DISCRIMINATOR) {
    @Override
    LegacyForgeHandshakeClientPhase nextPhase() {
      return WAITING_SERVER_DATA;
    }

    @Override
    boolean onHandle(ConnectedPlayer player,
        PluginMessage message,
        MinecraftConnection backendConn) {
      // Read the mod list if we haven't already.
      if (!player.getModInfo().isPresent()) {
        List<ModInfo.Mod> mods = LegacyForgeUtil.readModList(message);
        if (!mods.isEmpty()) {
          player.setModInfo(new ModInfo("FML", mods));
        }
      }

      return super.onHandle(player, message, backendConn);
    }
  },

  /**
   * Waiting for state data to be received. Transition to {@link #WAITING_SERVER_COMPLETE} when this
   * is complete and the client sends an ACK packet to confirm
   */
  WAITING_SERVER_DATA(LegacyForgeConstants.ACK_DISCRIMINATOR) {
    @Override
    LegacyForgeHandshakeClientPhase nextPhase() {
      return WAITING_SERVER_COMPLETE;
    }
  },

  /**
   * Waiting on the server to send another ACK. Transition to {@link #PENDING_COMPLETE} when client
   * sends another ACK
   */
  WAITING_SERVER_COMPLETE(LegacyForgeConstants.ACK_DISCRIMINATOR) {
    @Override
    LegacyForgeHandshakeClientPhase nextPhase() {
      return PENDING_COMPLETE;
    }
  },

  /**
   * Waiting on the server to send yet another ACK. Transition to {@link #COMPLETE} when client
   * sends another ACK
   */
  PENDING_COMPLETE(LegacyForgeConstants.ACK_DISCRIMINATOR) {
    @Override
    LegacyForgeHandshakeClientPhase nextPhase() {
      return COMPLETE;
    }
  },

  /**
   * The handshake is complete. The handshake can be reset.
   *
   * <p>Note that a successful connection to a server does not mean that we will be in this state.
   * After a handshake reset, if the next server is vanilla we will still be in the {@link
   * #NOT_STARTED} phase, which means we must NOT send a reset packet. This is handled by overriding
   * the {@link #resetConnectionPhase(ConnectedPlayer)} in this element (it is usually a no-op).
   */
  COMPLETE(null) {
    @Override
    public void resetConnectionPhase(ConnectedPlayer player) {
      player.getConnection().write(LegacyForgeUtil.resetPacket());
      player.setPhase(LegacyForgeHandshakeClientPhase.NOT_STARTED);
    }

    @Override
    public boolean consideredComplete() {
      return true;
    }

    @Override
    boolean onHandle(ConnectedPlayer player,
        PluginMessage message,
        MinecraftConnection backendConn) {
      super.onHandle(player, message, backendConn);

      // just in case the timing is awful
      player.sendKeepAlive();

      MinecraftSessionHandler handler = backendConn.getActiveSessionHandler();
      if (handler instanceof ClientPlaySessionHandler) {
        ((ClientPlaySessionHandler) handler).flushQueuedMessages();
      }

      return true;
    }
  };

  @Nullable
  private final Integer packetToAdvanceOn;

  /**
   * Creates an instance of the {@link LegacyForgeHandshakeClientPhase}.
   *
   * @param packetToAdvanceOn The ID of the packet discriminator that indicates that the client has
   *                          moved onto a new phase, and as such, Velocity should do so too
   *                          (inspecting {@link #nextPhase()}. A null indicates there is
   *                          no further phase to transition to.
   */
  LegacyForgeHandshakeClientPhase(Integer packetToAdvanceOn) {
    this.packetToAdvanceOn = packetToAdvanceOn;
  }

  @Override
  public final boolean handle(ConnectedPlayer player,
      PluginMessage message,
      VelocityServerConnection server) {
    if (server != null) {
      MinecraftConnection backendConn = server.getConnection();
      if (backendConn != null
          && message.getChannel().equals(LegacyForgeConstants.FORGE_LEGACY_HANDSHAKE_CHANNEL)) {
        // Get the phase and check if we need to start the next phase.
        LegacyForgeHandshakeClientPhase newPhase = getNewPhase(message);

        // Update phase on player
        player.setPhase(newPhase);

        // Perform phase handling
        return newPhase.onHandle(player, message, backendConn);
      }
    }

    // Not handled, fallback
    return false;
  }

  /**
   * Handles the phase tasks.
   *
   * @param player      The player
   * @param message     The message to handle
   * @param backendConn The backend connection to write to, if required.
   * @return true if handled, false otherwise.
   */
  boolean onHandle(ConnectedPlayer player,
      PluginMessage message,
      MinecraftConnection backendConn) {
    // Send the packet on to the server.
    backendConn.write(message.retain());

    // We handled the packet. No need to continue processing.
    return true;
  }

  @Override
  public boolean consideredComplete() {
    return false;
  }

  /**
   * Gets the next phase, if any (will return self if we are at the end of the handshake).
   *
   * @return The next phase
   */
  LegacyForgeHandshakeClientPhase nextPhase() {
    return this;
  }

  /**
   * Get the phase to act on, depending on the packet that has been sent.
   *
   * @param packet The packet
   * @return The phase to transition to, which may be the same as before.
   */
  private LegacyForgeHandshakeClientPhase getNewPhase(PluginMessage packet) {
    if (packetToAdvanceOn != null
        && LegacyForgeUtil.getHandshakePacketDiscriminator(packet) == packetToAdvanceOn) {
      return nextPhase();
    }

    return this;
  }
}
