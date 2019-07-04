package com.velocitypowered.proxy.command;

import com.google.common.collect.ImmutableList;
import com.velocitypowered.api.command.Command;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.permission.Tristate;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import java.util.List;
import java.util.Optional;
import net.kyori.text.Component;
import net.kyori.text.TextComponent;
import net.kyori.text.TranslatableComponent;
import net.kyori.text.format.TextColor;
import org.checkerframework.checker.nullness.qual.NonNull;

public class GlistCommand implements Command {

  private static final Component USAGE = TranslatableComponent
      .of("velocity.command.glist.usage");
  private static final Component TOO_MANY_ARGUMENTS = TranslatableComponent
      .of("velocity.command.too-many-arguments");

  private final ProxyServer server;

  public GlistCommand(ProxyServer server) {
    this.server = server;
  }

  @Override
  public void execute(CommandSource source, String @NonNull [] args) {
    if (args.length == 0) {
      sendTotalProxyCount(source);
      source.sendMessage(USAGE);
    } else if (args.length == 1) {
      String arg = args[0];
      if (arg.equalsIgnoreCase("all")) {
        for (RegisteredServer server : server.getAllServers()) {
          sendServerPlayers(source, server, true);
        }
        sendTotalProxyCount(source);
      } else {
        Optional<RegisteredServer> registeredServer = server.getServer(arg);
        if (!registeredServer.isPresent()) {
          source.sendMessage(TranslatableComponent
                  .of("velocity.command.glist.invalid-server", TextComponent.of(arg)));
          return;
        }
        sendServerPlayers(source, registeredServer.get(), false);
      }
    } else {
      source.sendMessage(TOO_MANY_ARGUMENTS);
    }
  }

  private void sendTotalProxyCount(CommandSource target) {
    int playersOnline = server.getAllPlayers().size();
    target.sendMessage(TranslatableComponent
        .of("velocity.command.glist.players-online", TextComponent.of(playersOnline)));
  }

  private void sendServerPlayers(CommandSource target, RegisteredServer server, boolean fromAll) {
    List<Player> onServer = ImmutableList.copyOf(server.getPlayersConnected());
    if (onServer.isEmpty() && fromAll) {
      return;
    }

    TextComponent.Builder builder = TextComponent.builder()
        .append(TextComponent.of("[" + server.getServerInfo().getName() + "] ",
            TextColor.DARK_AQUA))
        .append("(" + onServer.size() + ")", TextColor.GRAY)
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

  @Override
  public List<String> suggest(CommandSource source, String @NonNull [] currentArgs) {
    ImmutableList.Builder<String> options = ImmutableList.builder();
    for (RegisteredServer server : server.getAllServers()) {
      options.add(server.getServerInfo().getName());
    }
    options.add("all");

    switch (currentArgs.length) {
      case 0:
        return options.build();
      case 1:
        return options.build().stream()
            .filter(o -> o.regionMatches(true, 0, currentArgs[0], 0, currentArgs[0].length()))
            .collect(ImmutableList.toImmutableList());
      default:
        return ImmutableList.of();
    }
  }

  @Override
  public boolean hasPermission(CommandSource source, String @NonNull [] args) {
    return source.getPermissionValue("velocity.command.glist") == Tristate.TRUE;
  }
}
