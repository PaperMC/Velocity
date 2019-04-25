package com.velocitypowered.proxy.connection.client;

import static com.velocitypowered.proxy.connection.client.ClientPlaySessionHandler.MAX_PLUGIN_CHANNELS;

import com.velocitypowered.proxy.connection.MinecraftSessionHandler;
import com.velocitypowered.proxy.connection.backend.VelocityServerConnection;
import com.velocitypowered.proxy.protocol.packet.PluginMessage;
import com.velocitypowered.proxy.protocol.util.PluginMessageUtil;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class InitialConnectSessionHandler implements MinecraftSessionHandler {

  private final ConnectedPlayer player;
  private final Set<String> knownChannels;

  InitialConnectSessionHandler(ConnectedPlayer player) {
    this.player = player;
    this.knownChannels = new HashSet<>();
  }

  @Override
  public boolean handle(PluginMessage packet) {
    VelocityServerConnection serverConn = player.getConnectionInFlight();
    if (serverConn != null) {
      if (player.getPhase().handle(player, packet, serverConn)) {
        return true;
      }

      if (PluginMessageUtil.isRegister(packet)) {
        List<String> actuallyRegistered = new ArrayList<>();
        List<String> channels = PluginMessageUtil.getChannels(packet);
        for (String channel : channels) {
          if (knownChannels.size() >= MAX_PLUGIN_CHANNELS && !knownChannels.contains(channel)) {
            throw new IllegalStateException("Too many plugin message channels registered");
          }
          if (knownChannels.add(channel)) {
            actuallyRegistered.add(channel);
          }
        }

        if (!actuallyRegistered.isEmpty()) {
          PluginMessage newRegisterPacket = PluginMessageUtil.constructChannelsPacket(serverConn
              .ensureConnected().getProtocolVersion(), actuallyRegistered);
          serverConn.ensureConnected().write(newRegisterPacket);
        }
      } else if (PluginMessageUtil.isUnregister(packet)) {
        List<String> channels = PluginMessageUtil.getChannels(packet);
        knownChannels.removeAll(channels);
        serverConn.ensureConnected().write(packet);
      } else {
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

  Set<String> getKnownChannels() {
    return knownChannels;
  }
}
