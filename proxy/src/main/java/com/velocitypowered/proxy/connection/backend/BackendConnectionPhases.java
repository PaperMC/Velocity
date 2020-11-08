package com.velocitypowered.proxy.connection.backend;

import com.velocitypowered.proxy.connection.client.ConnectedPlayer;
import com.velocitypowered.proxy.connection.forge.legacy.LegacyForgeHandshakeBackendPhase;
import com.velocitypowered.proxy.network.packet.shared.PluginMessagePacket;

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
                          PluginMessagePacket message) {
      // The connection may be legacy forge. If so, the Forge handler will deal with this
      // for us. Otherwise, we have nothing to do.
      return LegacyForgeHandshakeBackendPhase.NOT_STARTED.handle(serverConn, player, message);
    }
  };

  /**
   * A special backend phase used to indicate that this connection is about to become
   * obsolete (transfer to a new server, for instance) and that Forge messages ought to be
   * forwarded on to an in-flight connection instead.
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
