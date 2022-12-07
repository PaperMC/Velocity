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

package com.velocitypowered.proxy.protocol.packet.chat.legacy;

import com.velocitypowered.api.event.command.CommandExecuteEvent;
import com.velocitypowered.proxy.VelocityServer;
import com.velocitypowered.proxy.connection.client.ConnectedPlayer;
import com.velocitypowered.proxy.protocol.packet.chat.CommandHandler;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;

public class LegacyCommandHandler implements CommandHandler<LegacyChat> {
  private final ConnectedPlayer player;
  private final VelocityServer server;

  public LegacyCommandHandler(ConnectedPlayer player, VelocityServer server) {
    this.player = player;
    this.server = server;
  }

  @Override
  public Class<LegacyChat> packetClass() {
    return LegacyChat.class;
  }

  @Override
  public void handlePlayerCommandInternal(LegacyChat packet) {
    String command = packet.getMessage().substring(1);
    queueCommandResult(this.server, this.player, event -> {
      CommandExecuteEvent.CommandResult result = event.getResult();
      if (!result.isAllowed()) {
        return CompletableFuture.completedFuture(null);
      }
      String commandToRun = result.getCommand().orElse(command);
      if (result.isForwardToServer()) {
        return CompletableFuture.completedFuture(this.player.getChatBuilderFactory().builder()
            .message("/" + commandToRun)
            .toServer());
      }
      return runCommand(this.server, this.player, commandToRun, hasRun -> {
        if (!hasRun) {
          return this.player.getChatBuilderFactory().builder()
              .message(packet.getMessage())
              .asPlayer(this.player)
              .toServer();
        }
        return null;
      });
    }, command, Instant.now());
  }
}
