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
import com.velocitypowered.proxy.VelocityServer;
import com.velocitypowered.proxy.connection.client.ConnectedPlayer;
import com.velocitypowered.proxy.protocol.MinecraftPacket;
import com.velocitypowered.proxy.protocol.packet.chat.ChatAcknowledgementPacket;
import com.velocitypowered.proxy.protocol.packet.chat.CommandHandler;
import java.util.concurrent.CompletableFuture;
import net.kyori.adventure.text.Component;
import org.checkerframework.checker.nullness.qual.Nullable;

public class SessionCommandHandler implements CommandHandler<SessionPlayerCommandPacket> {

  private final ConnectedPlayer player;
  private final VelocityServer server;

  public SessionCommandHandler(ConnectedPlayer player, VelocityServer server) {
    this.player = player;
    this.server = server;
  }

  @Override
  public Class<SessionPlayerCommandPacket> packetClass() {
    return SessionPlayerCommandPacket.class;
  }

  @Nullable
  private MinecraftPacket consumeCommand(SessionPlayerCommandPacket packet) {
    if (packet.lastSeenMessages != null) {
      return new ChatAcknowledgementPacket(packet.lastSeenMessages.getOffset());
    }
    return null;
  }

  @Nullable
  private MinecraftPacket forwardCommand(SessionPlayerCommandPacket packet, String newCommand) {
    if (packet.isSigned() && newCommand.equals(packet.command)) {
      return packet;
    }
    return modifyCommand(packet, newCommand);
  }

  @Nullable
  private MinecraftPacket modifyCommand(SessionPlayerCommandPacket packet, String newCommand) {
    if (packet.isSigned()) {
      logger.fatal("A plugin tried to change a command with signed component(s). "
          + "This is not supported. "
          + "Disconnecting player " + player.getUsername() + ". Command packet: " + packet);
      player.disconnect(Component.text(
          "A proxy plugin caused an illegal protocol state. "
              + "Contact your network administrator."));
      return null;
    }

    return this.player.getChatBuilderFactory()
        .builder()
        .setTimestamp(packet.timeStamp)
        .asPlayer(this.player)
        .message("/" + newCommand)
        .toServer();
  }

  @Override
  public void handlePlayerCommandInternal(SessionPlayerCommandPacket packet) {
    queueCommandResult(this.server, this.player, event -> {
      CommandExecuteEvent.CommandResult result = event.getResult();
      if (result == CommandExecuteEvent.CommandResult.denied()) {
        if (packet.isSigned()) {
          logger.fatal("A plugin tried to deny a command with signable component(s). "
              + "This is not supported. "
              + "Disconnecting player " + player.getUsername() + ". Command packet: " + packet);
          player.disconnect(Component.text(
              "A proxy plugin caused an illegal protocol state. "
                  + "Contact your network administrator."));
        }
        return CompletableFuture.completedFuture(consumeCommand(packet));
      }

      String commandToRun = result.getCommand().orElse(packet.command);
      if (result.isForwardToServer()) {
        return CompletableFuture.completedFuture(forwardCommand(packet, commandToRun));
      }

      return runCommand(this.server, this.player, commandToRun, hasRun -> {
        if (hasRun) {
          return consumeCommand(packet);
        }
        return forwardCommand(packet, commandToRun);
      });
    }, packet.command, packet.timeStamp);
  }
}
