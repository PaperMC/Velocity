package com.velocitypowered.proxy.connection.client;

import com.velocitypowered.proxy.connection.backend.VelocityServerConnection;
import com.velocitypowered.proxy.connection.forge.legacy.LegacyForgeHandshakeClientPhase;
import com.velocitypowered.proxy.network.packet.AbstractPluginMessagePacket;

/**
 * Provides connection phase specific actions.
 *
 * <p>Note that Forge phases are found in the enum
 * {@link LegacyForgeHandshakeClientPhase}.</p>
 */
public interface ClientConnectionPhase {

  /**
   * Handle a plugin message in the context of
   * this phase.
   *
   * @param player The player
   * @param message The message to handle
   * @param server The backend connection to use
   * @return true if handled, false otherwise.
   */
  default boolean handle(ConnectedPlayer player,
      AbstractPluginMessagePacket<?> message,
      VelocityServerConnection server) {
    return false;
  }

  /**
   * Instruct Velocity to reset the connection phase
   * back to its default for the connection type.
   *
   * @param player The player
   */
  default void resetConnectionPhase(ConnectedPlayer player) {
  }

  /**
   * Perform actions just as the player joins the
   * server.
   *
   * @param player The player
   */
  default void onFirstJoin(ConnectedPlayer player) {
  }

  /**
   * Indicates whether the connection is considered complete.
   * @return true if so
   */
  default boolean consideredComplete() {
    return true;
  }
}
