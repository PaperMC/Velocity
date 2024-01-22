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
import com.mojang.brigadier.tree.ArgumentCommandNode;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.permission.Tristate;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import java.util.Objects;
import java.util.Optional;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

/**
 * Implements the Velocity default {@code /send} command.
 */
public class SendCommand {
  private final ProxyServer server;
  private static final String SERVER_ARG = "server";
  private static final String PLAYER_ARG = "player";

  public SendCommand(ProxyServer server) {
    this.server = server;
  }

  /**
   * Registers this command.
   */
  public void register() {
    final LiteralArgumentBuilder<CommandSource> rootNode = BrigadierCommand
        .literalArgumentBuilder("send")
        .requires(source ->
            source.getPermissionValue("velocity.command.send") == Tristate.TRUE)
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
          if ("all".regionMatches(true, 0, argument, 0, argument.length())) {
            builder.suggest("all");
          }
          if ("current".regionMatches(true, 0, argument, 0, argument.length())
              && context.getSource() instanceof Player) {
            builder.suggest("current");
          }
          return builder.buildFuture();
        })
        .executes(this::usage);
    final ArgumentCommandNode<CommandSource, String> serverNode = BrigadierCommand
        .requiredArgumentBuilder(SERVER_ARG, StringArgumentType.word())
        .suggests((context, builder) -> {
          final String argument = context.getArguments().containsKey(SERVER_ARG)
              ? context.getArgument(SERVER_ARG, String.class)
              : "";
          for (final RegisteredServer server : server.getAllServers()) {
            final String serverName = server.getServerInfo().getName();
            if (serverName.regionMatches(true, 0, argument, 0, argument.length())) {
              builder.suggest(server.getServerInfo().getName());
            }
          }
          return builder.buildFuture();
        })
        .executes(this::send)
        .build();
    playerNode.then(serverNode);
    rootNode.then(playerNode.build());
    server.getCommandManager().register(new BrigadierCommand(rootNode.build()));
  }

  private int usage(final CommandContext<CommandSource> context) {
    context.getSource().sendMessage(
        Component.translatable("velocity.command.send-usage", NamedTextColor.YELLOW)
    );
    return Command.SINGLE_SUCCESS;
  }

  private int send(final CommandContext<CommandSource> context) {
    final String serverName = context.getArgument(SERVER_ARG, String.class);
    final String player = context.getArgument(PLAYER_ARG, String.class);

    final Optional<RegisteredServer> maybeServer = server.getServer(serverName);

    if (maybeServer.isEmpty()) {
      context.getSource().sendMessage(
          CommandMessages.SERVER_DOES_NOT_EXIST.arguments(Component.text(serverName))
      );
      return 0;
    }

    final RegisteredServer targetServer = maybeServer.get();

    final Optional<Player> maybePlayer = server.getPlayer(player);
    if (maybePlayer.isEmpty()
        && !Objects.equals(player, "all")
        && !Objects.equals(player, "current")) {
      context.getSource().sendMessage(
          CommandMessages.PLAYER_NOT_FOUND.arguments(Component.text(player))
      );
      return 0;
    }

    if (Objects.equals(player, "all")) {
      for (final Player p : server.getAllPlayers()) {
        p.createConnectionRequest(targetServer).fireAndForget();
      }
      return Command.SINGLE_SUCCESS;
    }

    if (Objects.equals(player, "current")) {
      if (!(context.getSource() instanceof Player source)) {
        context.getSource().sendMessage(CommandMessages.PLAYERS_ONLY);
        return 0;
      }

      final Optional<ServerConnection> connectedServer = source.getCurrentServer();
      if (connectedServer.isPresent()) {
        for (final Player p : connectedServer.get().getServer().getPlayersConnected()) {
          p.createConnectionRequest(maybeServer.get()).fireAndForget();
        }
        return Command.SINGLE_SUCCESS;
      }
      return 0;
    }

    // The player at this point must be present
    maybePlayer.orElseThrow().createConnectionRequest(targetServer).fireAndForget();
    return Command.SINGLE_SUCCESS;
  }
}
