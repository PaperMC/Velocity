/*
 * Copyright (C) 2018 Velocity Contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.velocitypowered.proxy.connection.client;

import com.velocitypowered.proxy.connection.MinecraftSessionHandler;
import com.velocitypowered.proxy.connection.backend.BungeeCordMessageResponder;
import com.velocitypowered.proxy.connection.backend.VelocityServerConnection;
import com.velocitypowered.proxy.protocol.packet.PluginMessage;
import com.velocitypowered.proxy.protocol.util.PluginMessageUtil;

public class InitialConnectSessionHandler implements MinecraftSessionHandler {

  private final ConnectedPlayer player;

  InitialConnectSessionHandler(ConnectedPlayer player) {
    this.player = player;
  }

  @Override
  public boolean handle(PluginMessage packet) {
    VelocityServerConnection serverConn = player.getConnectionInFlight();
    if (serverConn != null) {
      if (player.getPhase().handle(player, packet, serverConn)) {
        return true;
      }

      if (PluginMessageUtil.isRegister(packet)) {
        player.getKnownChannels().addAll(PluginMessageUtil.getChannels(packet));
      } else if (PluginMessageUtil.isUnregister(packet)) {
        player.getKnownChannels().removeAll(PluginMessageUtil.getChannels(packet));
      } else if (BungeeCordMessageResponder.isBungeeCordMessage(packet)) {
        return true;
      }
      serverConn.ensureConnected().write(packet.retain());
    }
    return true;
  }

  @Override
  public void disconnected() {
    // the user cancelled the login process
    player.teardown();
  }
}
