package com.velocitypowered.proxy.connection.client;

import com.velocitypowered.proxy.connection.MinecraftConnection;
import com.velocitypowered.proxy.connection.MinecraftSessionHandler;
import com.velocitypowered.proxy.protocol.MinecraftPacket;
import com.velocitypowered.proxy.protocol.StateRegistry;
import com.velocitypowered.proxy.protocol.packet.PluginMessage;

public class InitialConnectSessionHandler implements MinecraftSessionHandler {

  private final ConnectedPlayer player;

  InitialConnectSessionHandler(ConnectedPlayer player) {
    this.player = player;
  }

  @Override
  public void handleGeneric(MinecraftPacket packet) {
    // No-op: will never handle packets
  }

  @Override
  public boolean handle(PluginMessage packet) {
    if (player.getConnectionInFlight() != null) {
      MinecraftConnection connection = player.getConnectionInFlight().getConnection();
      if (connection != null && connection.getState() == StateRegistry.PLAY) {
        connection.write(packet);
      }
    }
    return true;
  }

  @Override
  public void disconnected() {
    // the user cancelled the login process
    player.teardown();
  }
}
