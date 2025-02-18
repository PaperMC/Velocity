/*
 * Copyright (C) 2020-2023 Velocity Contributors
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
import com.velocitypowered.proxy.plugin.virtual.VelocityVirtualPlugin;
import java.util.Optional;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

/**
 * Implements the Velocity default {@code /find} command.
 */
public class FindCommand {
  private final ProxyServer server;
  private static final String PLAYER_ARG = "player";

  public FindCommand(ProxyServer server) {
    this.server = server;
  }

  /**
   * Registers this command.
   */
  public void register() {
    final LiteralArgumentBuilder<CommandSource> rootNode = BrigadierCommand
        .literalArgumentBuilder("find")
        .requires(source ->
            source.getPermissionValue("velocity.command.find") == Tristate.TRUE)
        .executes(this::usage);
    final RequiredArgumentBuilder<CommandSource, String> playerNode = BrigadierCommand
        .requiredArgumentBuilder(PLAYER_ARG, StringArgumentType.word())
        .suggests((context, builder) -> {
          final String argument = context.getArguments().containsKey(PLAYER_ARG)
              ? context.getArgument(PLAYER_ARG, String.class)
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
    rootNode.then(playerNode.build());
    final BrigadierCommand command = new BrigadierCommand(rootNode);
    server.getCommandManager().register(
        server.getCommandManager().metaBuilder(command)
            .plugin(VelocityVirtualPlugin.INSTANCE)
            .build(),
        command
    );
  }

  private int usage(final CommandContext<CommandSource> context) {
    context.getSource().sendMessage(
        Component.translatable("velocity.command.find-usage", NamedTextColor.YELLOW)
    );
    return Command.SINGLE_SUCCESS;
  }

  private int find(final CommandContext<CommandSource> context) {
    final String playerArg = context.getArgument(PLAYER_ARG, String.class);

    final Optional<Player> maybePlayer = server.getPlayer(playerArg);
    if (maybePlayer.isEmpty()) {
      context.getSource().sendMessage(
          CommandMessages.PLAYER_NOT_FOUND.arguments(Component.text(playerArg))
      );
      return 0;
    }

    final Player player = maybePlayer.get();
    final Optional<ServerConnection> connection = player.getCurrentServer();
    if (connection.isEmpty()) {
      return 0;
    }

    context.getSource().sendMessage(
            Component.text(maybePlayer.get().getUsername() + " is connected to: " + connection.get().getServerInfo().getName(), NamedTextColor.YELLOW)
    );

    return Command.SINGLE_SUCCESS;
  }
}
