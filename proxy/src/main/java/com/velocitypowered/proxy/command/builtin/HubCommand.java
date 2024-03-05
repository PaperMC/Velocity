/*
 * Copyright (C) 2018-2024 Velocity Contributors
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

package com.velocitypowered.proxy.command.builtin;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.permission.Tristate;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.proxy.server.VelocityRegisteredServer;
import net.kyori.adventure.text.Component;

/**
 * Sends the server to the least populated fallback server.
 */
public class HubCommand {
  private final ProxyServer server;

  public HubCommand(ProxyServer server) {
    this.server = server;
  }

  /**
   * Register the command.
   */
  public BrigadierCommand register() {
    return new BrigadierCommand(BrigadierCommand
        .literalArgumentBuilder("hub")
        .requires(source ->
            source.getPermissionValue("velocity.command.hub") == Tristate.TRUE)
        .executes(this::lobby).build());
  }


  private int lobby(final CommandContext<CommandSource> context) {
    if (!(context.getSource() instanceof Player player)) {
      return 0;
    }

    ServerConnection connection = player.getCurrentServer().orElse(null);
    if (connection == null || connection.getServer() == null) {
      return 0;
    }

    RegisteredServer registeredServer = connection.getServer();
    if (server.getConfiguration().getAttemptConnectionOrder().contains(registeredServer.getServerInfo().getName())) {
      player.sendMessage(Component.translatable("velocity.command.hub.fallback-connected"));
      return 0;
    }

    if (registeredServer instanceof VelocityRegisteredServer velocityRegisteredServer) {
      RegisteredServer serverToTry = velocityRegisteredServer.getPlayer(player.getUniqueId()).getNextServerToTry().orElse(null);
      if (serverToTry == null) {
        return 0;
      }

      player.createConnectionRequest(serverToTry).connect();
      return Command.SINGLE_SUCCESS;
    }
    return 0;
  }
}
