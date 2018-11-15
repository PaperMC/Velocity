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
   * The backend connection is vanilla.
   */
  BackendConnectionPhase VANILLA = new BackendConnectionPhase() {};

  /**
   * The backend connection is unknown at this time.
   */
  BackendConnectionPhase UNKNOWN = new BackendConnectionPhase() {
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
   * Handle a plugin message in the context of
   * this phase.
   *
   * @param message The message to handle
   * @return true if handled, false otherwise.
   */
  default boolean handle(VelocityServerConnection server,
                         ConnectedPlayer player,
                         PluginMessage message) {
    return false;
  }

  /**
   * Indicates whether the connection is considered complete
   * @return true if so
   */
  default boolean consideredComplete() {
    return true;
  }

  /**
   * Fired when the provided server connection is about to be terminated
   * because the provided player is connecting to a new server.
   *
   * @param serverConnection The server the player is disconnecting from
   * @param player The player
   */
  default void onDepartForNewServer(VelocityServerConnection serverConnection,
                                    ConnectedPlayer player) {
  }
}
