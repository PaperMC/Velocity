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
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import net.kyori.text.TextComponent;
import net.kyori.text.TranslatableComponent;
import net.kyori.text.event.ClickEvent;
import net.kyori.text.event.HoverEvent;
import net.kyori.text.format.TextColor;
import org.checkerframework.checker.nullness.qual.NonNull;

public class ServerCommand implements Command {

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
        player.sendMessage(CommandMessages.serverDoesntExist(serverName));
        return;
      }

      player.createConnectionRequest(toConnect.get()).fireAndForget();
    } else {
      String currentServer = player.getCurrentServer().map(ServerConnection::getServerInfo)
          .map(ServerInfo::getName)
          .orElse("<unknown>");
      player.sendMessage(TranslatableComponent
          .of("velocity.command.server.currently-connected-to", TextComponent.of(currentServer)));

      // Assemble the list of servers as components
      TranslatableComponent.Builder serverListBuilder = TranslatableComponent
          .builder("velocity.command.server.available-servers");
      List<RegisteredServer> infos = ImmutableList.copyOf(server.getAllServers());
      for (int i = 0; i < infos.size(); i++) {
        RegisteredServer rs = infos.get(i);
        TextComponent serverName = TextComponent.of(rs.getServerInfo().getName());
        TextComponent playersOnline = TextComponent.of(rs.getPlayersConnected().size());
        TranslatableComponent infoComponent;
        if (rs.getServerInfo().getName().equals(currentServer)) {
          infoComponent = TranslatableComponent.builder("velocity.command.server.current-server")
              .args(serverName)
              .hoverEvent(HoverEvent.showText(TranslatableComponent
                  .of("velocity.command.server.current-server.hover", playersOnline)))
              .build();
        } else {
          infoComponent = TranslatableComponent.builder("velocity.command.server.other-server")
              .args(serverName)
              .clickEvent(ClickEvent.runCommand(
                  "/server " + rs.getServerInfo().getName()))
              .hoverEvent(HoverEvent.showText(TranslatableComponent
                  .of("velocity.command.server.other-server.hover", playersOnline)))
              .build();
        }
        serverListBuilder.append(infoComponent);
        if (i != infos.size() - 1) {
          serverListBuilder.append(TextComponent.of(", ", TextColor.GRAY));
        }
      }

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
