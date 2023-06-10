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
import java.util.function.Function;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public interface CommandHandler<T extends MinecraftPacket> {

  Logger logger = LogManager.getLogger(CommandHandler.class);

  Class<T> packetClass();

  void handlePlayerCommandInternal(T packet);

  default boolean handlePlayerCommand(MinecraftPacket packet) {
    if (packetClass().isInstance(packet)) {
      handlePlayerCommandInternal(packetClass().cast(packet));
      return true;
    }
    return false;
  }

  default CompletableFuture<MinecraftPacket> runCommand(VelocityServer server,
      ConnectedPlayer player, String command,
      Function<Boolean, MinecraftPacket> hasRunPacketFunction) {
    return server.getCommandManager().executeImmediatelyAsync(player, command)
        .thenApply(hasRunPacketFunction);
  }

  default void queueCommandResult(VelocityServer server, ConnectedPlayer player,
      Function<CommandExecuteEvent, CompletableFuture<MinecraftPacket>> futurePacketCreator,
      String message, Instant timestamp) {
    player.getChatQueue().queuePacket(
        server.getCommandManager().callCommandEvent(player, message)
            .thenComposeAsync(futurePacketCreator)
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
            }), timestamp);
  }
}
