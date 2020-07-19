package com.velocitypowered.proxy.command.builtin;

import static com.mojang.brigadier.arguments.StringArgumentType.getString;

import com.google.common.collect.ImmutableList;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.ArgumentCommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.permission.Tristate;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.proxy.command.VelocityCommandManager;
import java.util.List;
import java.util.Optional;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;

public class GlistCommand {

  private static final String SERVER_ARG = "server";

  private final ProxyServer server;

  public GlistCommand(ProxyServer server) {
    this.server = server;
    this.register();
  }

  private void register() {
    LiteralCommandNode<CommandSource> totalNode = LiteralArgumentBuilder
            .<CommandSource>literal("glist")
            .executes(this::totalCount)
            .build();

    ArgumentCommandNode<CommandSource, String> serverNode = RequiredArgumentBuilder
            .<CommandSource, String>argument("server", StringArgumentType.string())
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
    server.getCommandManager().brigadierBuilder().register(totalNode);
  }

  private int totalCount(final CommandContext<CommandSource> context) {
    final CommandSource source = context.getSource();
    if (!hasPermission(source)) return VelocityCommandManager.NO_PERMISSION;
    sendTotalProxyCount(source);
    source.sendMessage(
        TextComponent.builder("To view all players on servers, use ", NamedTextColor.YELLOW)
            .append("/glist all", NamedTextColor.DARK_AQUA)
            .append(".", NamedTextColor.YELLOW)
            .build());
    return 1;
  }

  private int serverCount(final CommandContext<CommandSource> context) {
    final CommandSource source = context.getSource();
    if (!hasPermission(source)) return VelocityCommandManager.NO_PERMISSION;
    final String serverName = getString(context, SERVER_ARG);
    if (serverName.equalsIgnoreCase("all")) {
      for (RegisteredServer server : BuiltinCommandUtil.sortedServerList(server)) {
        sendServerPlayers(source, server, true);
      }
      sendTotalProxyCount(source);
    } else {
      Optional<RegisteredServer> registeredServer = server.getServer(serverName);
      if (!registeredServer.isPresent()) {
        source.sendMessage(
          TextComponent.of("Server " + serverName + " doesn't exist.", NamedTextColor.RED));
        return -1;
      }
      sendServerPlayers(source, registeredServer.get(), false);
    }
    return 1;
  }

  private boolean hasPermission(final CommandSource source) {
    return source.getPermissionValue("velocity.command.glist") == Tristate.TRUE;
  }

  private void sendTotalProxyCount(CommandSource target) {
    target.sendMessage(TextComponent.builder("There are ", NamedTextColor.YELLOW)
        .append(Integer.toString(server.getAllPlayers().size()), NamedTextColor.GREEN)
        .append(" player(s) online.", NamedTextColor.YELLOW)
        .build());
  }

  private void sendServerPlayers(CommandSource target, RegisteredServer server, boolean fromAll) {
    List<Player> onServer = ImmutableList.copyOf(server.getPlayersConnected());
    if (onServer.isEmpty() && fromAll) {
      return;
    }

    TextComponent.Builder builder = TextComponent.builder()
        .append(TextComponent.of("[" + server.getServerInfo().getName() + "] ",
            NamedTextColor.DARK_AQUA))
        .append("(" + onServer.size() + ")", NamedTextColor.GRAY)
        .append(": ")
        .resetStyle();

    for (int i = 0; i < onServer.size(); i++) {
      Player player = onServer.get(i);
      builder.append(player.getUsername());

      if (i + 1 < onServer.size()) {
        builder.append(", ");
      }
    }

    target.sendMessage(builder.build());
  }
}
