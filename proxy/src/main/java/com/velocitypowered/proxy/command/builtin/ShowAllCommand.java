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
import com.velocitypowered.api.proxy.server.RegisteredServer;
import java.util.Optional;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

/**
 * Command to list all players that are currently connected to a server.
 */
public class ShowAllCommand {
  private final ProxyServer server;

  public ShowAllCommand(ProxyServer server) {
    this.server = server;
  }

  /**
   * Register the command.
   */
  public void register() {
    final LiteralArgumentBuilder<CommandSource> rootNode = BrigadierCommand
        .literalArgumentBuilder("showall")
        .requires(source ->
            source.getPermissionValue("velocity.command.showall") == Tristate.TRUE)
        .executes(this::usage);
    final RequiredArgumentBuilder<CommandSource, String> serverNode = BrigadierCommand
        .requiredArgumentBuilder("server", StringArgumentType.word())
        .suggests((context, builder) -> {
          final String argument = context.getArguments().containsKey("server")
              ? context.getArgument("server", String.class)
              : "";
          for (final RegisteredServer s : server.getAllServers()) {
            final String serverName = s.getServerInfo().getName();
            if (serverName.regionMatches(true, 0, argument, 0, argument.length())) {
              builder.suggest(serverName);
            }
          }
          return builder.buildFuture();
        })
        .executes(this::find);
    rootNode.then(serverNode);
    server.getCommandManager().register(new BrigadierCommand(rootNode.build()));
  }


  private int usage(final CommandContext<CommandSource> context) {
    context.getSource().sendMessage(
        Component.translatable("velocity.command.showall.usage", NamedTextColor.YELLOW)
    );
    return Command.SINGLE_SUCCESS;
  }

  private int find(final CommandContext<CommandSource> context) {
    final String serverName = context.getArgument("server", String.class);
    final Optional<RegisteredServer> maybeServer = server.getServer(serverName);
    if (maybeServer.isEmpty()) {
      context.getSource().sendMessage(
          CommandMessages.PLAYER_NOT_FOUND.arguments(Component.text(serverName))
      );
      return 0;
    }

    // can't be null, already checking if it's empty before
    RegisteredServer server = maybeServer.orElse(null);

    context.getSource().sendMessage(
        Component.translatable("velocity.command.showall.header", NamedTextColor.YELLOW)
    );

    StringBuilder builder = new StringBuilder();

    for (int i = 0; i < server.getPlayersConnected().size(); i++) {
      Player p = server.getPlayersConnected().stream().toList().get(i);
      builder.append(p.getUsername());
      if (i + 1 != server.getPlayersConnected().size()) {
        builder.append(", ");
      }
    }

    context.getSource().sendMessage(
        Component.translatable("velocity.command.showall.message", NamedTextColor.WHITE,
            Component.text(builder.toString()))
    );
    return Command.SINGLE_SUCCESS;
  }
}
