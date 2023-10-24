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

package com.velocitypowered.proxy.protocol.packet.chat.session;

import com.velocitypowered.api.event.command.CommandExecuteEvent;
import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.proxy.VelocityServer;
import com.velocitypowered.proxy.connection.client.ConnectedPlayer;
import com.velocitypowered.proxy.protocol.packet.chat.ChatAcknowledgement;
import com.velocitypowered.proxy.protocol.packet.chat.CommandHandler;

import java.util.concurrent.CompletableFuture;

public class SessionCommandHandler implements CommandHandler<SessionPlayerCommand> {

  private final ConnectedPlayer player;
  private final VelocityServer server;

  public SessionCommandHandler(ConnectedPlayer player, VelocityServer server) {
    this.player = player;
    this.server = server;
  }

  @Override
  public Class<SessionPlayerCommand> packetClass() {
    return SessionPlayerCommand.class;
  }

  @Override
  public void handlePlayerCommandInternal(SessionPlayerCommand packet) {
    queueCommandResult(this.server, this.player, event -> {
      CommandExecuteEvent.CommandResult result = event.getResult();
      if (result == CommandExecuteEvent.CommandResult.denied()) {
        if (packet.isSigned()) {
          logger.fatal("A plugin tried to deny a command with signable component(s). "
              + "This is not supported. "
              + "Disconnecting player " + player.getUsername() + ". Command packet: " + packet);
          /*player.disconnect(Component.text(
              "A proxy plugin caused an illegal protocol state. "
                  + "Contact your network administrator."));*/
        }
        // We seemingly can't actually do this if signed args exist, if not, we can probs keep stuff happy
        if (player.getProtocolVersion().compareTo(ProtocolVersion.MINECRAFT_1_19_3) >= 0) {
          return CompletableFuture.completedFuture(new ChatAcknowledgement(packet.lastSeenMessages.getOffset()));
        }
        return CompletableFuture.completedFuture(null);
      }

      String commandToRun = result.getCommand().orElse(packet.command);
      if (result.isForwardToServer()) {
        if (packet.isSigned() && commandToRun.equals(packet.command)) {
          return CompletableFuture.completedFuture(packet);
        } else {
          if (packet.isSigned()) {
            logger.fatal("A plugin tried to change a command with signed component(s). "
                + "This is not supported. "
                + "Disconnecting player " + player.getUsername() + ". Command packet: " + packet);
            /*player.disconnect(Component.text(
                "A proxy plugin caused an illegal protocol state. "
                    + "Contact your network administrator."));*/
            return CompletableFuture.completedFuture(null);
          }

          return CompletableFuture.completedFuture(this.player.getChatBuilderFactory()
              .builder()
              .setTimestamp(packet.timeStamp)
              .asPlayer(this.player)
              .message("/" + commandToRun)
              .toServer());
        }
      }

      return runCommand(this.server, this.player, commandToRun, hasRun -> {
        if (!hasRun) {
          if (packet.isSigned() && commandToRun.equals(packet.command)) {
            return packet;
          } else {
            if (packet.isSigned()) {
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
                .setTimestamp(packet.timeStamp)
                .asPlayer(this.player)
                .message("/" + commandToRun)
                .toServer();
          }
        }
        if (player.getProtocolVersion().compareTo(ProtocolVersion.MINECRAFT_1_19_3) >= 0) {
          return new ChatAcknowledgement(packet.lastSeenMessages.getOffset());
        }
        return null;
      });
    }, packet.command, packet.timeStamp);
  }
}
