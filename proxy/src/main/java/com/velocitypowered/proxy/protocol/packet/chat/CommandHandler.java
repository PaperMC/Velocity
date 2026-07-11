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

package com.velocitypowered.proxy.protocol.packet.chat;

import com.velocitypowered.api.event.command.CommandExecuteEvent;
import com.velocitypowered.proxy.VelocityServer;
import com.velocitypowered.proxy.connection.client.ConnectedPlayer;
import com.velocitypowered.proxy.protocol.MinecraftPacket;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;
import java.util.function.Function;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.checkerframework.checker.nullness.qual.Nullable;

public abstract class CommandHandler<T extends MinecraftPacket> {

  protected final Logger logger;

  protected CommandHandler() {
    this.logger = LogManager.getLogger(getClass());
  }

  protected abstract Class<T> packetClass();

  protected abstract void handlePlayerCommandInternal(T packet);

  public boolean handlePlayerCommand(MinecraftPacket packet) {
    if (packetClass().isInstance(packet)) {
      handlePlayerCommandInternal(packetClass().cast(packet));
      return true;
    }
    return false;
  }

  protected CompletableFuture<MinecraftPacket> runCommand(VelocityServer server,
      ConnectedPlayer player, String command,
      Function<Boolean, MinecraftPacket> hasRunPacketFunction) {
    return server.getCommandManager().executeImmediatelyAsync(player, command)
        .thenApply(hasRunPacketFunction);
  }

  protected void queueCommandResult(VelocityServer server, ConnectedPlayer player,
      BiFunction<CommandExecuteEvent, LastSeenMessages, CompletableFuture<MinecraftPacket>> futurePacketCreator,
      String message, Instant timestamp, @Nullable LastSeenMessages lastSeenMessages,
                                  CommandExecuteEvent.InvocationInfo invocationInfo) {
      CompletableFuture<CommandExecuteEvent> eventFuture = server.getCommandManager().callCommandEvent(player, message,
              invocationInfo);
      player.getChatQueue().queuePacket(
        newLastSeenMessages -> eventFuture
            .thenComposeAsync(event -> futurePacketCreator.apply(event, newLastSeenMessages))
            .thenApply(pkt -> {
              if (server.getConfiguration().isLogCommandExecutions()) {
                logger.info("{} -> executed command /{}", player, message);
              }
              return pkt;
            }).exceptionally(e -> {
              logger.info("Exception occurred while running command for {}", player.getUsername(), e);
              player.sendMessage(
                  Component.translatable("velocity.command.generic-error", NamedTextColor.RED));
              return null;
            }), timestamp, lastSeenMessages);
  }

  protected void alterSignableComponentError(String what, ConnectedPlayer player, MinecraftPacket packet) {
      logger.fatal("A plugin tried to " + what + " a command with signable component(s). "
              + "This is not supported. "
              + "Disconnecting player " + player.getUsername() + ". Command packet: " + packet);
      player.disconnect(Component.text(
              "A proxy plugin caused an illegal protocol state. "
                      + "Contact your network administrator."));
  }
}
