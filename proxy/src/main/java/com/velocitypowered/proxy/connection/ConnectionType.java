package com.velocitypowered.proxy.connection;

import com.velocitypowered.api.util.GameProfile;
import com.velocitypowered.proxy.config.PlayerInfoForwarding;
import com.velocitypowered.proxy.connection.backend.BackendConnectionPhase;
import com.velocitypowered.proxy.connection.client.ClientConnectionPhase;
import com.velocitypowered.proxy.connection.forge.legacy.LegacyForgeConnectionType;
import com.velocitypowered.proxy.connection.util.ConnectionTypeImpl;

/**
 * The types of connection that may be selected.
 */
public interface ConnectionType {

  /**
   * Indicates that the connection has yet to reach the
   * point where we have a definitive answer as to what
   * type of connection we have.
   */
  ConnectionType UNDETERMINED =
      new ConnectionTypeImpl(ClientConnectionPhase.VANILLA, BackendConnectionPhase.UNKNOWN);

  /**
   * Indicates that the connection is a Vanilla connection.
   */
  ConnectionType VANILLA =
      new ConnectionTypeImpl(ClientConnectionPhase.VANILLA, BackendConnectionPhase.VANILLA);

  /**
   * Indicates that the connection is a 1.8-1.12 Forge
   * connection.
   */
  ConnectionType LEGACY_FORGE = new LegacyForgeConnectionType();

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
