package com.velocitypowered.proxy.connection.player;

import com.velocitypowered.proxy.connection.client.ConnectedPlayer;

public class LegacyResourcePackManager implements ResourcePackManager {
  private final ConnectedPlayer connectedPlayer;

  public LegacyResourcePackManager(ConnectedPlayer connectedPlayer) {
    this.connectedPlayer = connectedPlayer;
  }
}
