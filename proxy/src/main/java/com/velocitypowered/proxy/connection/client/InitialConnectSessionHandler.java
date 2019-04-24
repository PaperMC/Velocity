package com.velocitypowered.proxy.connection.client;

import com.velocitypowered.proxy.connection.MinecraftSessionHandler;
import com.velocitypowered.proxy.connection.backend.VelocityServerConnection;
import com.velocitypowered.proxy.protocol.packet.PluginMessage;

public class InitialConnectSessionHandler implements MinecraftSessionHandler {

  private final ConnectedPlayer player;

  InitialConnectSessionHandler(ConnectedPlayer player) {
    this.player = player;
  }

  @Override
  public boolean handle(PluginMessage packet) {
    VelocityServerConnection serverConn = player.getConnectionInFlight();
    if (serverConn != null) {
      if (!player.getPhase().handle(player, packet, serverConn)) {
        serverConn.ensureConnected().write(packet);
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
