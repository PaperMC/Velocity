package com.velocitypowered.proxy.command;

import com.google.common.collect.ImmutableList;
import com.velocitypowered.api.command.Command;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.permission.Tristate;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.proxy.text.TextJoiner;
import com.velocitypowered.proxy.text.translation.Translatable;
import java.util.List;
import java.util.Optional;
import net.kyori.text.Component;
import net.kyori.text.TranslatableComponent;
import org.checkerframework.checker.nullness.qual.NonNull;

public class GlistCommand implements Command {

  private static final Component USAGE = TranslatableComponent
      .of("velocity.command.glist.usage");

  private static final Translatable PLAYERS_ONLINE = Translatable
      .of("velocity.command.glist.players-online");

  private static final Translatable PLAYER_LIST = Translatable
      .of("velocity.command.glist.player-list");
  private static final Translatable PLAYER_LIST_ENTRY = Translatable
      .of("velocity.command.glist.player-list.entry");
  private static final TextJoiner PLAYER_LIST_ENTRY_JOINER = TextJoiner
      .on(TranslatableComponent.of("velocity.command.glist.player-list.entry.separator"));

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
          source.sendMessage(CommandMessages.SERVER_DOESNT_EXIST.with(arg));
          return;
        }
        sendServerPlayers(source, registeredServer.get(), false);
      }
    } else {
      source.sendMessage(CommandMessages.TOO_MANY_ARGUMENTS.get());
    }
  }

  private void sendTotalProxyCount(CommandSource target) {
    int playersOnline = server.getAllPlayers().size();
    target.sendMessage(PLAYERS_ONLINE.with(playersOnline));
  }

  private void sendServerPlayers(CommandSource target, RegisteredServer server, boolean fromAll) {
    List<Player> onServer = ImmutableList.copyOf(server.getPlayersConnected());
    if (onServer.isEmpty() && fromAll) {
      return;
    }

    Component playersComponent = PLAYER_LIST_ENTRY_JOINER.join(onServer.stream()
        .map(player -> PLAYER_LIST_ENTRY.with(player.getUsername())));
    Component playerListComponent = PLAYER_LIST
        .with(server.getServerInfo().getName(), onServer.size(), playersComponent);

    target.sendMessage(playerListComponent);
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
