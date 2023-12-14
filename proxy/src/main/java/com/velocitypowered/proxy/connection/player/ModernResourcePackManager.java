package com.velocitypowered.proxy.connection.player;

import com.velocitypowered.proxy.connection.client.ConnectedPlayer;

public class ModernResourcePackManager implements ResourcePackManager {
  private final ConnectedPlayer connectedPlayer;

  public ModernResourcePackManager(ConnectedPlayer connectedPlayer) {
    this.connectedPlayer = connectedPlayer;
  }
}
