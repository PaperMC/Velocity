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
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.permission.Tristate;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import java.util.Optional;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

/**
 * Command to find what server a player is connected to.
 */
public class FindCommand {
  private final ProxyServer server;

  public FindCommand(ProxyServer server) {
    this.server = server;
  }

  /**
   * Register the command.
   */
  public void register() {
    final LiteralArgumentBuilder<CommandSource> rootNode = BrigadierCommand
        .literalArgumentBuilder("find")
        .requires(source ->
          source.getPermissionValue("velocity.command.find") == Tristate.TRUE)
        .executes(this::usage);
    final RequiredArgumentBuilder<CommandSource, String> playerNode = BrigadierCommand
        .requiredArgumentBuilder("player", StringArgumentType.word())
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
        .executes(this::find);
    rootNode.then(playerNode);
    server.getCommandManager().register(new BrigadierCommand(rootNode.build()));
  }


  private int usage(final CommandContext<CommandSource> context) {
    context.getSource().sendMessage(
        Component.translatable("velocity.command.find.usage", NamedTextColor.YELLOW)
    );
    return Command.SINGLE_SUCCESS;
  }

  private int find(final CommandContext<CommandSource> context) {
    final String player = context.getArgument("player", String.class);
    final Optional<Player> maybePlayer = server.getPlayer(player);
    if (maybePlayer.isEmpty()) {
      context.getSource().sendMessage(
          CommandMessages.PLAYER_NOT_FOUND.arguments(Component.text(player))
      );
      return 0;
    }

    // can't be null, already checking if it's empty before
    Player p = maybePlayer.get();
    ServerConnection connection = p.getCurrentServer().orElse(null);
    if (connection == null) {
      context.getSource().sendMessage(
          Component.translatable("velocity.command.find.no-server", NamedTextColor.YELLOW)
      );
      return 0;
    }

    RegisteredServer server = connection.getServer();
    if (server == null) {
      context.getSource().sendMessage(
          Component.translatable("velocity.command.find.no-server", NamedTextColor.YELLOW)
      );
      return 0;
    }

    context.getSource().sendMessage(
        Component.translatable("velocity.command.find.message", NamedTextColor.YELLOW,
            Component.text(player), Component.text(server.getServerInfo().getName()))
    );
    return Command.SINGLE_SUCCESS;
  }
}
