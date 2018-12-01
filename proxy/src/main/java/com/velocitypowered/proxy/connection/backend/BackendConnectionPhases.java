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
  public static final BackendConnectionPhase VANILLA = new BackendConnectionPhase() {};

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

  private BackendConnectionPhases() {
    throw new AssertionError();
  }
}
