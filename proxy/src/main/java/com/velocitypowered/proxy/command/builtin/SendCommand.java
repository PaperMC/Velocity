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

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.ArgumentCommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
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
    LiteralCommandNode<CommandSource> totalNode = LiteralArgumentBuilder
        .<CommandSource>literal("send")
        .requires(source ->
            source.getPermissionValue("velocity.command.send") == Tristate.TRUE)
        .executes(this::usage)
        .build();
    ArgumentCommandNode<CommandSource, String> playerNode = RequiredArgumentBuilder
        .<CommandSource, String>argument("player", StringArgumentType.word())
        .suggests((context, builder) -> {
          String argument = context.getArguments().containsKey(PLAYER_ARG)
              ? context.getArgument(PLAYER_ARG, String.class)
              : "";
          for (Player player : server.getAllPlayers()) {
            String playerName = player.getUsername();
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
        .executes(this::usage)
        .build();
    ArgumentCommandNode<CommandSource, String> serverNode = RequiredArgumentBuilder
        .<CommandSource, String>argument("server", StringArgumentType.word())
        .suggests((context, builder) -> {
          String argument = context.getArguments().containsKey(SERVER_ARG)
              ? context.getArgument(SERVER_ARG, String.class)
              : "";
          for (RegisteredServer server : server.getAllServers()) {
            String serverName = server.getServerInfo().getName();
            if (serverName.regionMatches(true, 0, argument, 0, argument.length())) {
              builder.suggest(server.getServerInfo().getName());
            }
          }
          return builder.buildFuture();
        })
        .executes(this::send)
        .build();
    totalNode.addChild(playerNode);
    playerNode.addChild(serverNode);
    server.getCommandManager().register(new BrigadierCommand(totalNode));
  }

  private int usage(CommandContext<CommandSource> context) {
    context.getSource().sendMessage(
        Component.translatable("velocity.command.send-usage", NamedTextColor.YELLOW)
    );
    return 1;
  }

  private int send(CommandContext<CommandSource> context) {
    String serverName = context.getArgument(SERVER_ARG, String.class);
    String player = context.getArgument(PLAYER_ARG, String.class);

    Optional<RegisteredServer> maybeServer = server.getServer(serverName);

    if (maybeServer.isEmpty()) {
      context.getSource().sendMessage(
          CommandMessages.SERVER_DOES_NOT_EXIST.args(Component.text(serverName))
      );
      return 0;
    }

    if (server.getPlayer(player).isEmpty()
        && !Objects.equals(player, "all")
        && !Objects.equals(player, "current")) {
      context.getSource().sendMessage(
          CommandMessages.PLAYER_NOT_FOUND.args(Component.text(player))
      );
      return 0;
    }

    if (Objects.equals(player, "all")) {
      for (Player p : server.getAllPlayers()) {
        p.createConnectionRequest(server.getServer(serverName).get()).fireAndForget();
      }
      return 1;
    }

    if (Objects.equals(player, "current")) {
      if (!(context.getSource() instanceof Player)) {
        context.getSource().sendMessage(CommandMessages.PLAYERS_ONLY);
        return 0;
      }

      Player source = (Player) context.getSource();
      Optional<ServerConnection> connectedServer = source.getCurrentServer();
      if (connectedServer.isPresent()) {
        for (Player p : connectedServer.get().getServer().getPlayersConnected()) {
          p.createConnectionRequest(maybeServer.get()).fireAndForget();
        }
        return 1;
      }
      return 0;
    }

    server.getPlayer(player).get().createConnectionRequest(
        maybeServer.get()).fireAndForget();
    return 1;
  }
}
