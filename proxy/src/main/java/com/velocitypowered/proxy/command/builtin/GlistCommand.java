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
import java.util.List;
import java.util.Optional;
import net.kyori.adventure.identity.Identity;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.TranslatableComponent;
import net.kyori.adventure.text.format.NamedTextColor;

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
    LiteralCommandNode<CommandSource> totalNode = LiteralArgumentBuilder
        .<CommandSource>literal("glist")
        .requires(source ->
            source.getPermissionValue("velocity.command.glist") == Tristate.TRUE)
        .executes(this::totalCount)
        .build();
    ArgumentCommandNode<CommandSource, String> serverNode = RequiredArgumentBuilder
        .<CommandSource, String>argument(SERVER_ARG, StringArgumentType.string())
        .suggests((context, builder) -> {
          for (RegisteredServer server : server.getAllServers()) {
            builder.suggest(server.getServerInfo().getName());
          }
          builder.suggest("all");
          return builder.buildFuture();
        })
        .executes(this::serverCount)
        .build();
    totalNode.addChild(serverNode);
    server.getCommandManager().register(new BrigadierCommand(totalNode));
  }

  private int totalCount(final CommandContext<CommandSource> context) {
    final CommandSource source = context.getSource();
    sendTotalProxyCount(source);
    source.sendMessage(Identity.nil(),
        Component.translatable("velocity.command.glist-view-all", NamedTextColor.YELLOW));
    return 1;
  }

  private int serverCount(final CommandContext<CommandSource> context) {
    final CommandSource source = context.getSource();
    final String serverName = getString(context, SERVER_ARG);
    if (serverName.equalsIgnoreCase("all")) {
      for (RegisteredServer server : BuiltinCommandUtil.sortedServerList(server)) {
        sendServerPlayers(source, server, true);
      }
      sendTotalProxyCount(source);
    } else {
      Optional<RegisteredServer> registeredServer = server.getServer(serverName);
      if (!registeredServer.isPresent()) {
        source.sendMessage(Identity.nil(),
            CommandMessages.SERVER_DOES_NOT_EXIST.args(Component.text(serverName)));
        return -1;
      }
      sendServerPlayers(source, registeredServer.get(), false);
    }
    return 1;
  }

  private void sendTotalProxyCount(CommandSource target) {
    int online = server.getPlayerCount();
    TranslatableComponent msg = online == 1
        ? Component.translatable("velocity.command.glist-player-singular")
        : Component.translatable("velocity.command.glist-player-plural");
    target.sendMessage(msg.color(NamedTextColor.YELLOW)
        .args(Component.text(Integer.toString(online), NamedTextColor.GREEN)));
  }

  private void sendServerPlayers(CommandSource target, RegisteredServer server, boolean fromAll) {
    List<Player> onServer = ImmutableList.copyOf(server.getPlayersConnected());
    if (onServer.isEmpty() && fromAll) {
      return;
    }

    TextComponent.Builder builder = Component.text()
        .append(Component.text("[" + server.getServerInfo().getName() + "] ",
            NamedTextColor.DARK_AQUA))
        .append(Component.text("(" + onServer.size() + ")", NamedTextColor.GRAY))
        .append(Component.text(": "))
        .resetStyle();

    for (int i = 0; i < onServer.size(); i++) {
      Player player = onServer.get(i);
      builder.append(Component.text(player.getUsername()));

      if (i + 1 < onServer.size()) {
        builder.append(Component.text(", "));
      }
    }

    target.sendMessage(Identity.nil(), builder.build());
  }
}
