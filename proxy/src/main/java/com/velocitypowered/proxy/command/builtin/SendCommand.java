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
import com.velocitypowered.api.proxy.server.RegisteredServer;
import java.util.Objects;
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
              for (Player player : server.getAllPlayers()) {
                builder.suggest(player.getUsername());
              }
              builder.suggest("all");
              return builder.buildFuture();
            })
            .executes(this::usage)
            .build();
    ArgumentCommandNode<CommandSource, String> serverNode = RequiredArgumentBuilder
            .<CommandSource, String>argument("server", StringArgumentType.word())
            .suggests((context, builder) -> {
              for (RegisteredServer server : server.getAllServers()) {
                builder.suggest(server.getServerInfo().getName());
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

    if (server.getServer(serverName).isEmpty()) {
      context.getSource().sendMessage(
              CommandMessages.SERVER_DOES_NOT_EXIST.args(Component.text(serverName))
      );
      return 0;
    }

    if (server.getPlayer(player).isEmpty() && !Objects.equals(player, "all")) {
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

    server.getPlayer(player).get().createConnectionRequest(
            server.getServer(serverName).get()
    ).fireAndForget();
    return 1;
  }
}
