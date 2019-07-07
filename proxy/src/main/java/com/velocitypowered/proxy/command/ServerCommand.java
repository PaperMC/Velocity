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
import com.velocitypowered.proxy.text.translation.Translatable;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.kyori.text.Component;
import net.kyori.text.TranslatableComponent;
import net.kyori.text.event.ClickEvent;
import net.kyori.text.event.HoverEvent;
import org.checkerframework.checker.nullness.qual.NonNull;

public class ServerCommand implements Command {

  private static final Translatable CURRENTLY_CONNECTED_TO = Translatable
      .of("velocity.command.server.currently-connected-to");
  private static final Translatable AVAILABLE_SERVERS = Translatable
      .of("velocity.command.server.available-servers");
  private static final Translatable CURRENT_SERVER = Translatable
      .of("velocity.command.server.current-server");
  private static final Translatable CURRENT_SERVER_HOVER = Translatable
      .of("velocity.command.server.current-server.hover");
  private static final Translatable OTHER_SERVER = Translatable
      .of("velocity.command.server.other-server");
  private static final Translatable OTHER_SERVER_HOVER = Translatable
      .of("velocity.command.server.other-server.hover");

  private final ProxyServer server;

  public ServerCommand(ProxyServer server) {
    this.server = server;
  }

  @Override
  public void execute(CommandSource source, String @NonNull [] args) {
    if (!(source instanceof Player)) {
      source.sendMessage(CommandMessages.ONLY_PLAYERS_CAN_EXECUTE.get());
      return;
    }

    Player player = (Player) source;
    if (args.length == 1) {
      // Trying to connect to a server.
      String serverName = args[0];
      Optional<RegisteredServer> toConnect = server.getServer(serverName);
      if (!toConnect.isPresent()) {
        player.sendMessage(CommandMessages.SERVER_DOESNT_EXIST.with(serverName));
        return;
      }

      player.createConnectionRequest(toConnect.get()).fireAndForget();
    } else {
      String currentServer = player.getCurrentServer().map(ServerConnection::getServerInfo)
          .map(ServerInfo::getName)
          .orElse("<unknown>");
      player.sendMessage(CURRENTLY_CONNECTED_TO.with(currentServer));

      // Assemble the list of servers as components
      TranslatableComponent.Builder serverListBuilder = AVAILABLE_SERVERS.builder();

      Stream<Component> infos = ImmutableList.copyOf(server.getAllServers()).stream()
          .map(rs -> {
            String serverName = rs.getServerInfo().getName();
            int playersOnline = rs.getPlayersConnected().size();
            if (serverName.equals(currentServer)) {
              return CURRENT_SERVER
                  .builderWith(serverName)
                  .hoverEvent(HoverEvent.showText(CURRENT_SERVER_HOVER.with(playersOnline)))
                  .build();
            } else {
              return OTHER_SERVER
                  .builderWith(serverName)
                  .clickEvent(ClickEvent.runCommand(
                      "/server " + rs.getServerInfo().getName()))
                  .hoverEvent(HoverEvent.showText(OTHER_SERVER_HOVER.with(playersOnline)))
                  .build();
            }
          });
      TextJoiner.on(", ").appendTo(serverListBuilder, infos);

      player.sendMessage(serverListBuilder.build());
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
