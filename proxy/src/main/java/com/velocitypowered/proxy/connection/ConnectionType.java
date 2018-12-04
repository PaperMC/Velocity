package com.velocitypowered.proxy.connection;

import com.velocitypowered.api.util.GameProfile;
import com.velocitypowered.proxy.config.PlayerInfoForwarding;
import com.velocitypowered.proxy.connection.backend.BackendConnectionPhase;
import com.velocitypowered.proxy.connection.client.ClientConnectionPhase;

/**
 * The types of connection that may be selected.
 */
public interface ConnectionType {

  /**
   * The initial {@link ClientConnectionPhase} for this connection type.
   *
   * @return The {@link ClientConnectionPhase}
   */
  ClientConnectionPhase getInitialClientPhase();

  /**
   * The initial {@link BackendConnectionPhase} for this connection type.
   *
   * @return The {@link BackendConnectionPhase}
   */
  BackendConnectionPhase getInitialBackendPhase();

  /**
   * Adds properties to the {@link GameProfile} if required. If any properties
   * are added, the returned {@link GameProfile} will be a copy.
   *
   * @param original The original {@link GameProfile}
   * @param forwardingType The Velocity {@link PlayerInfoForwarding}
   * @return The {@link GameProfile} with the properties added in.
   */
  GameProfile addGameProfileTokensIfRequired(GameProfile original,
                                             PlayerInfoForwarding forwardingType);

  /**
   * Tests whether the hostname is the handshake packet is valid.
   *
   * @param address The address to check
   * @return true if valid.
   */
  default boolean checkServerAddressIsValid(String address) {
    return !address.contains("\0");
  }
}
