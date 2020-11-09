package com.velocitypowered.proxy.connection.backend;

import com.velocitypowered.proxy.connection.client.ConnectedPlayer;
import com.velocitypowered.proxy.connection.forge.legacy.LegacyForgeHandshakeBackendPhase;
import com.velocitypowered.proxy.network.packet.AbstractPluginMessagePacket;

/**
 * Provides connection phase specific actions.
 *
 * <p>Note that Forge phases are found in the enum
 * {@link LegacyForgeHandshakeBackendPhase}.</p>
 */
public interface BackendConnectionPhase {

  /**
   * Handle a plugin message in the context of
   * this phase.
   *
   * @param message The message to handle
   * @return true if handled, false otherwise.
   */
  default boolean handle(VelocityServerConnection server,
                         ConnectedPlayer player,
                         AbstractPluginMessagePacket<?> message) {
    return false;
  }

  /**
   * Indicates whether the connection is considered complete.
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
