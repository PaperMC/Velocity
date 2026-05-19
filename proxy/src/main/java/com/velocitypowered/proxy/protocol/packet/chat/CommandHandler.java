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

/**
 * Handles inbound player command packets of a specific type.
 *
 * @param <T> the command packet type handled by this handler
 */
public interface CommandHandler<T extends MinecraftPacket> {

  Logger logger = LogManager.getLogger(CommandHandler.class);

  Class<T> packetClass();

  void handlePlayerCommandInternal(T packet);

  /**
   * Handles the given packet if it matches this handler's packet type.
   *
   * @param packet the packet to handle
   * @return {@code true} if the packet was handled by this handler
   */
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

  /**
   * Runs the command event and queues the resulting packet so it is sent in chat order.
   *
   * @param server the proxy server
   * @param player the player who issued the command
   * @param futurePacketCreator builds the packet to forward once the command event completes
   * @param message the raw command text
   * @param timestamp the time the command was issued
   * @param lastSeenMessages the last-seen messages reported with the command, or {@code null}
   * @param invocationInfo the command invocation metadata
   */
  default void queueCommandResult(VelocityServer server, ConnectedPlayer player,
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
}
