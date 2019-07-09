package com.velocitypowered.proxy.command;

import com.google.common.collect.ImmutableList;
import com.velocitypowered.api.command.Command;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.permission.Tristate;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.proxy.server.ServerInfo;
import com.velocitypowered.proxy.text.TextJoiner;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import net.kyori.text.Component;
import net.kyori.text.TextComponent;
import net.kyori.text.TranslatableComponent;
import net.kyori.text.event.ClickEvent;
import net.kyori.text.event.HoverEvent;
import org.checkerframework.checker.nullness.qual.NonNull;

public class ServerCommand implements Command {

  private static final TranslatableComponent CURRENTLY_CONNECTED_TO = TranslatableComponent
      .of("velocity.command.server.currently-connected-to");
  private static final TranslatableComponent SERVER_LIST = TranslatableComponent
      .of("velocity.command.server.server-list");
  private static final TextJoiner SERVER_LIST_ENTRY_JOINER = TextJoiner.on(TranslatableComponent
      .of("velocity.command.server.server-list.entry.separator"));
  private static final TranslatableComponent CURRENT_SERVER = TranslatableComponent
      .of("velocity.command.server.server-list.entry.current");
  private static final TranslatableComponent CURRENT_SERVER_HOVER = TranslatableComponent
      .of("velocity.command.server.server-list.entry.current.hover");
  private static final TranslatableComponent OTHER_SERVER = TranslatableComponent
      .of("velocity.command.server.server-list.entry.other");
  private static final TranslatableComponent OTHER_SERVER_HOVER = TranslatableComponent
      .of("velocity.command.server.server-list.entry.other.hover");

  private final ProxyServer server;

  public ServerCommand(ProxyServer server) {
    this.server = server;
  }

  @Override
  public void execute(CommandSource source, String @NonNull [] args) {
    if (!(source instanceof Player)) {
      source.sendMessage(CommandMessages.ONLY_PLAYERS_CAN_EXECUTE);
      return;
    }

    Player player = (Player) source;
    if (args.length == 1) {
      // Trying to connect to a server.
      String serverName = args[0];
      Optional<RegisteredServer> toConnect = server.getServer(serverName);
      if (!toConnect.isPresent()) {
        player.sendMessage(CommandMessages.SERVER_DOESNT_EXIST
            .args(TextComponent.of(serverName)));
        return;
      }

      player.createConnectionRequest(toConnect.get()).fireAndForget();
    } else {
      String currentServer = player.getCurrentServer().map(ServerConnection::getServerInfo)
          .map(ServerInfo::getName)
          .orElse("<unknown>");
      player.sendMessage(CURRENTLY_CONNECTED_TO.args(TextComponent.of(currentServer)));

      Component entriesComponent = SERVER_LIST_ENTRY_JOINER
          .join(ImmutableList.copyOf(server.getAllServers()).stream()
              .map(rs -> {
                String serverName = rs.getServerInfo().getName();
                TextComponent serverNameComponent = TextComponent.of(serverName);
                TextComponent playersOnline = TextComponent.of(rs.getPlayersConnected().size());
                if (serverName.equals(currentServer)) {
                  return CURRENT_SERVER.toBuilder()
                      .args(serverNameComponent)
                      .hoverEvent(HoverEvent.showText(CURRENT_SERVER_HOVER.args(playersOnline)))
                      .build();
                } else {
                  return OTHER_SERVER.toBuilder()
                      .args(serverNameComponent)
                      .clickEvent(ClickEvent.runCommand(
                          "/server " + rs.getServerInfo().getName()))
                      .hoverEvent(HoverEvent.showText(OTHER_SERVER_HOVER.args(playersOnline)))
                      .build();
                }
              }));
      Component serverListComponent = SERVER_LIST.args(entriesComponent);

      player.sendMessage(serverListComponent);
    }
  }

  @Override
  public List<String> suggest(CommandSource source, String @NonNull [] currentArgs) {
    if (currentArgs.length == 0) {
      return server.getAllServers().stream()
          .map(rs -> rs.getServerInfo().getName())
          .collect(Collectors.toList());
    } else if (currentArgs.length == 1) {
      return server.getAllServers().stream()
          .map(rs -> rs.getServerInfo().getName())
          .filter(name -> name.regionMatches(true, 0, currentArgs[0], 0, currentArgs[0].length()))
          .collect(Collectors.toList());
    } else {
      return ImmutableList.of();
    }
  }

  @Override
  public boolean hasPermission(CommandSource source, String @NonNull [] args) {
    return source.getPermissionValue("velocity.command.server") != Tristate.FALSE;
  }
}
