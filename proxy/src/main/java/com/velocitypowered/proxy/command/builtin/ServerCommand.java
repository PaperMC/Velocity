/*
 * Copyright (C) 2018-2023 Velocity Contributors
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

import static net.kyori.adventure.text.event.HoverEvent.showText;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.permission.Tristate;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.proxy.server.ServerInfo;
import java.util.List;
import java.util.Optional;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.TranslatableComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.translation.Argument;

/**
 * Implements Velocity's {@code /server} command.
 */
public final class ServerCommand {
  private static final String SERVER_ARG = "server";
  public static final int MAX_SERVERS_TO_LIST = 50;

  @SuppressWarnings("checkstyle:MissingJavadocMethod")
  public static BrigadierCommand create(final ProxyServer server) {
    final LiteralCommandNode<CommandSource> node = BrigadierCommand
        .literalArgumentBuilder("server")
        .requires(src -> src instanceof Player
                && src.getPermissionValue("velocity.command.server") != Tristate.FALSE)
        .executes(ctx -> {
          final Player player = (Player) ctx.getSource();
          outputServerInformation(player, server);
          return Command.SINGLE_SUCCESS;
        })
        .then(BrigadierCommand.requiredArgumentBuilder(SERVER_ARG, StringArgumentType.word())
            .suggests((ctx, builder) -> {
              final String argument = ctx.getArguments().containsKey(SERVER_ARG)
                      ? StringArgumentType.getString(ctx, SERVER_ARG)
                      : "";
              for (final RegisteredServer sv : server.getAllServers()) {
                final String serverName = sv.getServerInfo().getName();
                if (serverName.regionMatches(true, 0, argument, 0, argument.length())) {
                  builder.suggest(serverName);
                }
              }
              return builder.buildFuture();
            })
            .executes(ctx -> {
              final Player player = (Player) ctx.getSource();
              // Trying to connect to a server.
              final String serverName = StringArgumentType.getString(ctx, SERVER_ARG);
              final Optional<RegisteredServer> toConnect = server.getServer(serverName);
              if (toConnect.isEmpty()) {
                player.sendMessage(CommandMessages.SERVER_DOES_NOT_EXIST
                        .arguments(Argument.string("server", serverName)));
                return -1;
              }

              player.createConnectionRequest(toConnect.get()).fireAndForget();
              return Command.SINGLE_SUCCESS;
            })
        ).build();

    return new BrigadierCommand(node);
  }

  private static void outputServerInformation(final Player executor,
                                              final ProxyServer server) {
    final String currentServer = executor.getCurrentServer()
        .map(ServerConnection::getServerInfo)
        .map(ServerInfo::getName)
        .orElse("<unknown>");
    executor.sendMessage(Component.translatable(
        "velocity.command.server-current-server",
        NamedTextColor.YELLOW,
        Component.text(currentServer)));

    final List<RegisteredServer> servers = BuiltinCommandUtil.sortedServerList(server);
    if (servers.size() > MAX_SERVERS_TO_LIST) {
      executor.sendMessage(Component.translatable(
          "velocity.command.server-too-many", NamedTextColor.RED));
      return;
    }

    // Assemble the list of servers as components
    final TextComponent.Builder serverListBuilder = Component.text()
        .append(Component.translatable("velocity.command.server-available",
            NamedTextColor.YELLOW))
        .appendSpace();
    for (int i = 0; i < servers.size(); i++) {
      final RegisteredServer rs = servers.get(i);
      serverListBuilder.append(formatServerComponent(currentServer, rs));
      if (i != servers.size() - 1) {
        serverListBuilder.append(Component.text(", ", NamedTextColor.GRAY));
      }
    }

    executor.sendMessage(serverListBuilder.build());
  }

  private static TextComponent formatServerComponent(final String currentPlayerServer,
                                              final RegisteredServer server) {
    final ServerInfo serverInfo = server.getServerInfo();
    final TextComponent.Builder serverTextComponent = Component.text()
            .content(serverInfo.getName());

    final int connectedPlayers = server.getPlayersConnected().size();
    final TranslatableComponent.Builder playersTextComponent = Component.translatable();
    if (connectedPlayers == 1) {
      playersTextComponent.key("velocity.command.server-tooltip-player-online");
    } else {
      playersTextComponent.key("velocity.command.server-tooltip-players-online");
    }
    playersTextComponent.arguments(Argument.component("players", Component.text(connectedPlayers)));
    if (serverInfo.getName().equals(currentPlayerServer)) {
      serverTextComponent.color(NamedTextColor.GREEN)
          .hoverEvent(
              showText(
                  Component.translatable("velocity.command.server-tooltip-current-server")
                      .append(Component.newline())
                      .append(playersTextComponent))
          );
    } else {
      serverTextComponent.color(NamedTextColor.GRAY)
          .clickEvent(ClickEvent.runCommand("/server " + serverInfo.getName()))
          .hoverEvent(
              showText(
                  Component.translatable("velocity.command.server-tooltip-offer-connect-server")
                      .append(Component.newline())
                      .append(playersTextComponent))
          );
    }
    return serverTextComponent.build();
  }
}
