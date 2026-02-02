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

import static com.mojang.brigadier.arguments.StringArgumentType.getString;

import com.google.common.collect.ImmutableList;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.ArgumentCommandNode;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.permission.Tristate;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.proxy.plugin.virtual.VelocityVirtualPlugin;
import java.util.List;
import java.util.Optional;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.TranslatableComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.translation.Argument;

/**
 * Implements the Velocity default {@code /glist} command.
 */
public class GlistCommand {

  private static final String SERVER_ARG = "server";

  private final ProxyServer server;

  public GlistCommand(ProxyServer server) {
    this.server = server;
  }

  /**
   * Registers this command.
   */
  public void register() {
    final LiteralArgumentBuilder<CommandSource> rootNode = BrigadierCommand
        .literalArgumentBuilder("glist")
        .requires(source ->
            source.getPermissionValue("velocity.command.glist") == Tristate.TRUE)
        .executes(this::totalCount);
    final ArgumentCommandNode<CommandSource, String> serverNode = BrigadierCommand
        .requiredArgumentBuilder(SERVER_ARG, StringArgumentType.string())
        .suggests((context, builder) -> {
          final String argument = context.getArguments().containsKey(SERVER_ARG)
              ? context.getArgument(SERVER_ARG, String.class)
              : "";
          for (RegisteredServer server : server.getAllServers()) {
            final String serverName = server.getServerInfo().getName();
            if (serverName.regionMatches(true, 0, argument, 0, argument.length())) {
              builder.suggest(serverName);
            }
          }
          if ("all".regionMatches(true, 0, argument, 0, argument.length())) {
            builder.suggest("all");
          }
          return builder.buildFuture();
        })
        .executes(this::serverCount)
        .build();
    rootNode.then(serverNode);
    final BrigadierCommand command = new BrigadierCommand(rootNode);
    server.getCommandManager().register(
        server.getCommandManager().metaBuilder(command)
            .plugin(VelocityVirtualPlugin.INSTANCE)
            .build(),
        command
    );
  }

  private int totalCount(final CommandContext<CommandSource> context) {
    final CommandSource source = context.getSource();
    sendTotalProxyCount(source);
    source.sendMessage(
        Component.translatable("velocity.command.glist-view-all", NamedTextColor.YELLOW));
    return 1;
  }

  private int serverCount(final CommandContext<CommandSource> context) {
    final CommandSource source = context.getSource();
    final String serverName = getString(context, SERVER_ARG);
    if (serverName.equalsIgnoreCase("all")) {
      for (final RegisteredServer server : BuiltinCommandUtil.sortedServerList(server)) {
        sendServerPlayers(source, server, true);
      }
      sendTotalProxyCount(source);
    } else {
      final Optional<RegisteredServer> registeredServer = server.getServer(serverName);
      if (registeredServer.isEmpty()) {
        source.sendMessage(
            CommandMessages.SERVER_DOES_NOT_EXIST
                    .arguments(Argument.string("server", serverName)));
        return -1;
      }
      sendServerPlayers(source, registeredServer.get(), false);
    }
    return Command.SINGLE_SUCCESS;
  }

  private void sendTotalProxyCount(CommandSource target) {
    final int online = server.getPlayerCount();
    final TranslatableComponent.Builder msg = Component.translatable()
            .key(online == 1
                  ? "velocity.command.glist-player-singular"
                  : "velocity.command.glist-player-plural"
            ).color(NamedTextColor.YELLOW)
            .arguments(Argument.component(
                    "players", Component.text(Integer.toString(online), NamedTextColor.GREEN)));
    target.sendMessage(msg.build());
  }

  private void sendServerPlayers(final CommandSource target,
                                 final RegisteredServer server, final boolean fromAll) {
    final List<Player> onServer = ImmutableList.copyOf(server.getPlayersConnected());
    if (onServer.isEmpty() && fromAll) {
      return;
    }

    final TextComponent.Builder builder = Component.text()
        .append(Component.text("[" + server.getServerInfo().getName() + "] ",
            NamedTextColor.DARK_AQUA))
        .append(Component.text("(" + onServer.size() + ")", NamedTextColor.GRAY))
        .append(Component.text(": "))
        .resetStyle();

    for (int i = 0; i < onServer.size(); i++) {
      final Player player = onServer.get(i);
      builder.append(Component.text(player.getUsername()));

      if (i + 1 < onServer.size()) {
        builder.append(Component.text(", "));
      }
    }

    target.sendMessage(builder.build());
  }
}
