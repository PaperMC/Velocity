package com.velocitypowered.proxy.connection.client;

/**
 * The vanilla {@link ClientConnectionPhase}s.
 *
 * <p>See {@link com.velocitypowered.proxy.connection.forge.legacy.LegacyForgeHandshakeClientPhase}
 * for Legacy Forge phases</p>
 */
public final class ClientConnectionPhases {

  /**
   * The client is connecting with a vanilla client (as far as we can tell).
   */
  public static final ClientConnectionPhase VANILLA = new ClientConnectionPhase() {};

  private ClientConnectionPhases() {
    throw new AssertionError();
  }
}
