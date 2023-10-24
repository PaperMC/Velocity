/*
 * Copyright (C) 2022-2023 Velocity Contributors
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

package com.velocitypowered.proxy.protocol.packet.chat.keyed;

import com.velocitypowered.api.event.command.CommandExecuteEvent;
import com.velocitypowered.api.proxy.crypto.IdentifiedKey;
import com.velocitypowered.proxy.VelocityServer;
import com.velocitypowered.proxy.connection.client.ConnectedPlayer;
import com.velocitypowered.proxy.protocol.packet.chat.CommandHandler;
import com.velocitypowered.proxy.protocol.packet.chat.builder.ChatBuilderV2;

import java.util.concurrent.CompletableFuture;

public class KeyedCommandHandler implements CommandHandler<KeyedPlayerCommand> {

  private final ConnectedPlayer player;
  private final VelocityServer server;

  public KeyedCommandHandler(ConnectedPlayer player, VelocityServer server) {
    this.player = player;
    this.server = server;
  }

  @Override
  public Class<KeyedPlayerCommand> packetClass() {
    return KeyedPlayerCommand.class;
  }

  @Override
  public void handlePlayerCommandInternal(KeyedPlayerCommand packet) {
    queueCommandResult(this.server, this.player, event -> {
      CommandExecuteEvent.CommandResult result = event.getResult();
      IdentifiedKey playerKey = player.getIdentifiedKey();
      if (result == CommandExecuteEvent.CommandResult.denied()) {
        if (playerKey != null) {
          if (!packet.isUnsigned()
              && playerKey.getKeyRevision().compareTo(IdentifiedKey.Revision.LINKED_V2) >= 0) {
            logger.fatal("A plugin tried to deny a command with signable component(s). "
                + "This is not supported. "
                + "Disconnecting player " + player.getUsername() + ". Command packet: " + packet);
            /* player.disconnect(Component.text("A proxy plugin caused an illegal protocol state. "+ "Contact your network administrator."));*/
          }
        }
        return CompletableFuture.completedFuture(null);
      }

      String commandToRun = result.getCommand().orElse(packet.getCommand());
      if (result.isForwardToServer()) {
        ChatBuilderV2 write = this.player.getChatBuilderFactory()
            .builder()
            .setTimestamp(packet.getTimestamp())
            .asPlayer(this.player);

        if (!packet.isUnsigned() && commandToRun.equals(packet.getCommand())) {
          return CompletableFuture.completedFuture(packet);
        } else {
          if (!packet.isUnsigned() && playerKey != null
              && playerKey.getKeyRevision().compareTo(IdentifiedKey.Revision.LINKED_V2) >= 0) {
            logger.fatal("A plugin tried to change a command with signed component(s). "
                + "This is not supported. "
                + "Disconnecting player " + player.getUsername() + ". Command packet: " + packet);
            /*player.disconnect(Component.text(
                "A proxy plugin caused an illegal protocol state. "
                    + "Contact your network administrator."));*/
            return CompletableFuture.completedFuture(null);
          }
          write.message("/" + commandToRun);
        }
        return CompletableFuture.completedFuture(write.toServer());
      }
      return runCommand(this.server, this.player, commandToRun, hasRun -> {
        if (!hasRun) {
          if (commandToRun.equals(packet.getCommand())) {
            return packet;
          }

          if (!packet.isUnsigned() && playerKey != null
              && playerKey.getKeyRevision().compareTo(IdentifiedKey.Revision.LINKED_V2) >= 0) {
            logger.fatal("A plugin tried to change a command with signed component(s). "
                + "This is not supported. "
                + "Disconnecting player " + player.getUsername() + ". Command packet: " + packet);
            /*player.disconnect(Component.text(
                "A proxy plugin caused an illegal protocol state. "
                    + "Contact your network administrator."));*/
            return null;
          }

          return this.player.getChatBuilderFactory()
              .builder()
              .setTimestamp(packet.getTimestamp())
              .asPlayer(this.player)
              .message("/" + commandToRun)
              .toServer();
        }
        return null;
      });
    }, packet.getCommand(), packet.getTimestamp());
  }
}
