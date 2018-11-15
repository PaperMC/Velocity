package com.velocitypowered.proxy.connection.forge.legacy;

import com.velocitypowered.api.util.ModInfo;
import com.velocitypowered.proxy.connection.MinecraftConnection;
import com.velocitypowered.proxy.connection.backend.VelocityServerConnection;
import com.velocitypowered.proxy.connection.client.ClientConnectionPhase;
import com.velocitypowered.proxy.connection.client.ClientPlaySessionHandler;
import com.velocitypowered.proxy.connection.client.ConnectedPlayer;
import com.velocitypowered.proxy.protocol.packet.PluginMessage;
import javax.annotation.Nullable;
import java.util.List;

/**
 * Allows for simple tracking of the phase that the Legacy
 * Forge handshake is in
 */
public enum LegacyForgeHandshakeClientPhase implements ClientConnectionPhase {

  /**
   * No handshake packets have yet been sent.
   * Transition to {@link #HELLO} when the ClientHello is sent.
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
  },

  /**
   * Client and Server exchange pleasantries.
   * Transition to {@link #MOD_LIST} when the ModList is sent.
   */
  HELLO(LegacyForgeConstants.MOD_LIST_DISCRIMINATOR) {
    @Override
    LegacyForgeHandshakeClientPhase nextPhase() {
      return MOD_LIST;
    }
  },



  /**
   * The Mod list is sent to the server, captured by Velocity.
   * Transition to {@link #WAITING_SERVER_DATA} when an ACK is sent, which
   * indicates to the server to start sending state data.
   */
  MOD_LIST(LegacyForgeConstants.ACK_DISCRIMINATOR) {
    @Override
    LegacyForgeHandshakeClientPhase nextPhase() {
      return WAITING_SERVER_DATA;
    }

    @Override
    void preWrite(ConnectedPlayer player, PluginMessage packet) {
      // Read the mod list if we haven't already.
      if (!player.getModInfo().isPresent()) {
        List<ModInfo.Mod> mods = LegacyForgeUtil.readModList(packet);
        if (!mods.isEmpty()) {
          player.setModInfo(new ModInfo("FML", mods));
        }
      }
    }
  },

  /**
   * Waiting for state data to be received.
   * Transition to {@link #WAITING_SERVER_COMPLETE} when this is complete
   * and the client sends an ACK packet to confirm
   */
  WAITING_SERVER_DATA(LegacyForgeConstants.ACK_DISCRIMINATOR) {
    @Override
    LegacyForgeHandshakeClientPhase nextPhase() {
      return WAITING_SERVER_COMPLETE;
    }
  },

  /**
   * Waiting on the server to send another ACK.
   * Transition to {@link #PENDING_COMPLETE} when client sends another
   * ACK
   */
  WAITING_SERVER_COMPLETE(LegacyForgeConstants.ACK_DISCRIMINATOR) {
    @Override
    LegacyForgeHandshakeClientPhase nextPhase() {
      return PENDING_COMPLETE;
    }
  },

  /**
   * Waiting on the server to send yet another ACK.
   * Transition to {@link #COMPLETE} when client sends another
   * ACK
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
   * <p>Note that a successful connection to a server does not mean that
   * we will be in this state. After a handshake reset, if the next server
   * is vanilla we will still be in the {@link #NOT_STARTED} phase,
   * which means we must NOT send a reset packet. This is handled by
   * overriding the {@link #resetConnectionPhase(ConnectedPlayer)} in this
   * element (it is usually a no-op).</p>
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
    void postWrite(ConnectedPlayer player, ClientPlaySessionHandler handler) {
      // just in case the timing is awful
      player.sendKeepAlive();
      handler.flushQueuedMessages();
    }
  };

  @Nullable private final Integer packetToAdvanceOn;

  /**
   * Creates an instance of the {@link LegacyForgeHandshakeClientPhase}.
   *
   * @param packetToAdvanceOn The ID of the packet discriminator that indicates
   *                          that the client has moved onto a new phase, and
   *                          as such, Velocity should do so too (inspecting
   *                          {@link #nextPhase()}. A null indicates there is no
   *                          further phase to transition to.
   */
  LegacyForgeHandshakeClientPhase(Integer packetToAdvanceOn) {
    this.packetToAdvanceOn = packetToAdvanceOn;
  }

  @Override
  public final boolean handle(ConnectedPlayer player,
                              ClientPlaySessionHandler handler,
                              PluginMessage message) {
    VelocityServerConnection serverConn = player.getConnectedServer();
    if (serverConn != null) {
      MinecraftConnection backendConn = serverConn.getConnection();
      if (backendConn != null
          && message.getChannel().equals(LegacyForgeConstants.FORGE_LEGACY_HANDSHAKE_CHANNEL)) {
        // Get the phase and check if we need to start the next phase.
        LegacyForgeHandshakeClientPhase newPhase = getNewPhase(message);

        // Update phase on player
        player.setPhase(newPhase);

        // Perform tasks before sending the packet on to the server.
        newPhase.preWrite(player, message);

        // Send the packet on to the server.
        backendConn.write(message);

        // Perform tasks after sending the packet on, such as keep alives.
        newPhase.postWrite(player, handler);

        // We handled the packet, nothing else needs to.
        return true;
      }
    }

    // Not handled, fallback
    return false;
  }

  @Override
  public boolean consideredComplete() {
    return false;
  }

  /**
   * Actions to occur before the handled packet is sent on to
   * the server.
   *
   * @param player The player to act on
   * @param packet The packet that was sent
   */
  void preWrite(ConnectedPlayer player, PluginMessage packet) {
    // usually nothing to do.
  }

  /**
   * Actions to occur after the handled packet is sent on to the
   * server.
   *
   * @param player The player
   * @param handler The {@link ClientPlaySessionHandler} to act with
   */
  void postWrite(ConnectedPlayer player, ClientPlaySessionHandler handler) {
    // usually nothing to do
  }

  /**
   * Gets the next phase, if any (will return self if we are at the end
   * of the handshake).
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
