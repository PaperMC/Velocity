package com.velocitypowered.proxy.connection.util;

import com.velocitypowered.api.proxy.config.PlayerInfoForwarding;
import com.velocitypowered.api.util.GameProfile;
import com.velocitypowered.proxy.connection.ConnectionType;
import com.velocitypowered.proxy.connection.backend.BackendConnectionPhase;
import com.velocitypowered.proxy.connection.client.ClientConnectionPhase;

/**
 * Indicates the type of connection that has been made.
 */
public class ConnectionTypeImpl implements ConnectionType {

  private final ClientConnectionPhase initialClientPhase;
  private final BackendConnectionPhase initialBackendPhase;

  public ConnectionTypeImpl(ClientConnectionPhase initialClientPhase,
                            BackendConnectionPhase initialBackendPhase) {
    this.initialClientPhase = initialClientPhase;
    this.initialBackendPhase = initialBackendPhase;
  }

  @Override
  public final ClientConnectionPhase getInitialClientPhase() {
    return initialClientPhase;
  }

  @Override
  public final BackendConnectionPhase getInitialBackendPhase() {
    return initialBackendPhase;
  }

  @Override
  public GameProfile addGameProfileTokensIfRequired(GameProfile original,
                                                    PlayerInfoForwarding forwardingType) {
    return original;
  }
}

