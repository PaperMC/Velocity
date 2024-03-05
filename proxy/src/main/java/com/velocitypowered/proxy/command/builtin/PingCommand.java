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
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.permission.Tristate;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import java.util.Optional;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

/**
 * Implements a {@code /ping} command to return the ping of a given player (in ms).
 */
public class PingCommand {
  private final ProxyServer server;

  public PingCommand(ProxyServer server) {
    this.server = server;
  }

  /**
   * Registers this command.
   */
  public void register() {
    LiteralArgumentBuilder<CommandSource> node = BrigadierCommand.literalArgumentBuilder("ping")
        .requires(source -> source.getPermissionValue("velocity.command.ping") != Tristate.FALSE)
        .then(
            BrigadierCommand.requiredArgumentBuilder("player", StringArgumentType.word())
                .suggests((context, builder) -> {
                  final String argument = context.getArguments().containsKey("player")
                      ? context.getArgument("player", String.class)
                      : "";

                  for (final Player player : server.getAllPlayers()) {
                    final String playerName = player.getUsername();
                    if (playerName.regionMatches(true, 0, argument, 0, argument.length())) {
                      builder.suggest(playerName);
                    }
                  }

                  return builder.buildFuture();
                })
                .executes(context -> {
                  String player = context.getArgument("player", String.class);
                  Optional<Player> maybePlayer = server.getPlayer(player);

                  if (maybePlayer.isEmpty()) {
                    context.getSource().sendMessage(
                        CommandMessages.PLAYER_NOT_FOUND.arguments(Component.text(player))
                    );
                    return 0;
                  }

                  return this.getPing(context, maybePlayer.get());
                })
        )
        .executes(context -> {
          if (context.getSource() instanceof Player player) {
            return this.getPing(context, player);
          } else {
            context.getSource().sendMessage(CommandMessages.PLAYERS_ONLY);
            return 0;
          }
        });

    server.getCommandManager().register(new BrigadierCommand(node.build()));
  }

  private int getPing(CommandContext<CommandSource> context, Player player) {
    long ping = player.getPing();

    if (ping == -1L) {
      context.getSource().sendMessage(
          Component.translatable("velocity.command.ping.unknown", NamedTextColor.RED)
              .arguments(Component.text(player.getUsername()))
      );

      return 0;
    }

    // Check if sending player matches for the response message.
    boolean matchesSender = false;

    if (context.getSource() instanceof Player sendingPlayer) {
      if (player.getUniqueId().equals(sendingPlayer.getUniqueId())) {
        matchesSender = true;
      }
    }

    if (matchesSender) {
      context.getSource().sendMessage(
          Component.translatable("velocity.command.ping.self", NamedTextColor.GREEN)
              .arguments(Component.text(ping))
      );
    } else {
      context.getSource().sendMessage(
          Component.translatable("velocity.command.ping.other", NamedTextColor.GREEN)
              .arguments(Component.text(player.getUsername()), Component.text(ping))
      );
    }

    return Command.SINGLE_SUCCESS;
  }
}
