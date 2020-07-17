package com.velocitypowered.proxy.command.builtin;

import com.google.common.collect.ImmutableList;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.LegacyCommand;
import com.velocitypowered.api.command.LegacyCommandInvocation;
import com.velocitypowered.api.permission.Tristate;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.proxy.server.ServerInfo;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;

import static net.kyori.adventure.text.event.HoverEvent.showText;

public class ServerCommand implements LegacyCommand {

  public static final int MAX_SERVERS_TO_LIST = 50;
  private final ProxyServer server;

  public ServerCommand(ProxyServer server) {
    this.server = server;
  }

  @Override
  public void execute(final LegacyCommandInvocation invocation) {
    final CommandSource source = invocation.source();
    final String[] args = invocation.arguments();

    if (!(source instanceof Player)) {
      source.sendMessage(TextComponent.of("Only players may run this command.",
          NamedTextColor.RED));
      return;
    }

    Player player = (Player) source;
    if (args.length == 1) {
      // Trying to connect to a server.
      String serverName = args[0];
      Optional<RegisteredServer> toConnect = server.getServer(serverName);
      if (!toConnect.isPresent()) {
        player.sendMessage(
            TextComponent.of("Server " + serverName + " doesn't exist.", NamedTextColor.RED));
        return;
      }

      player.createConnectionRequest(toConnect.get()).fireAndForget();
    } else {
      outputServerInformation(player);
    }
  }

  private void outputServerInformation(Player executor) {
    String currentServer = executor.getCurrentServer().map(ServerConnection::getServerInfo)
        .map(ServerInfo::getName).orElse("<unknown>");
    executor.sendMessage(TextComponent.of("You are currently connected to " + currentServer + ".",
        NamedTextColor.YELLOW));

    List<RegisteredServer> servers = BuiltinCommandUtil.sortedServerList(server);
    if (servers.size() > MAX_SERVERS_TO_LIST) {
      executor.sendMessage(TextComponent.of(
          "Too many servers to list. Tab-complete to show all servers.", NamedTextColor.RED));
      return;
    }

    // Assemble the list of servers as components
    TextComponent.Builder serverListBuilder = TextComponent.builder("Available servers: ")
        .color(NamedTextColor.YELLOW);
    for (int i = 0; i < servers.size(); i++) {
      RegisteredServer rs = servers.get(i);
      serverListBuilder.append(formatServerComponent(currentServer, rs));
      if (i != servers.size() - 1) {
        serverListBuilder.append(TextComponent.of(", ", NamedTextColor.GRAY));
      }
    }

    executor.sendMessage(serverListBuilder.build());
  }

  private TextComponent formatServerComponent(String currentPlayerServer, RegisteredServer server) {
    ServerInfo serverInfo = server.getServerInfo();
    TextComponent serverTextComponent = TextComponent.of(serverInfo.getName());

    String playersText = server.getPlayersConnected().size() + " player(s) online";
    if (serverInfo.getName().equals(currentPlayerServer)) {
      serverTextComponent = serverTextComponent.color(NamedTextColor.GREEN)
          .hoverEvent(
              showText(TextComponent.of("Currently connected to this server\n" + playersText))
          );
    } else {
      serverTextComponent = serverTextComponent.color(NamedTextColor.GRAY)
          .clickEvent(ClickEvent.runCommand("/server " + serverInfo.getName()))
          .hoverEvent(
              showText(TextComponent.of("Click to connect to this server\n" + playersText))
          );
    }
    return serverTextComponent;
  }

  @Override
  public List<String> suggest(final LegacyCommandInvocation invocation) {
    final String[] currentArgs = invocation.arguments();
    Stream<String> possibilities = Stream.concat(Stream.of("all"), server.getAllServers()
        .stream().map(rs -> rs.getServerInfo().getName()));

    if (currentArgs.length == 0) {
      return possibilities.collect(Collectors.toList());
    } else if (currentArgs.length == 1) {
      return possibilities
          .filter(name -> name.regionMatches(true, 0, currentArgs[0], 0, currentArgs[0].length()))
          .collect(Collectors.toList());
    } else {
      return ImmutableList.of();
    }
  }

  @Override
  public boolean hasPermission(final LegacyCommandInvocation invocation) {
    return invocation.source().getPermissionValue("velocity.command.server") != Tristate.FALSE;
  }
}
