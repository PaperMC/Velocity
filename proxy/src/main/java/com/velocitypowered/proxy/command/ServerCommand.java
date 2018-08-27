package com.velocitypowered.proxy.command;

import com.google.common.collect.ImmutableList;
import com.velocitypowered.api.command.Command;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.server.ServerInfo;
import net.kyori.text.TextComponent;
import net.kyori.text.event.ClickEvent;
import net.kyori.text.event.HoverEvent;
import net.kyori.text.format.TextColor;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class ServerCommand implements Command {
    private final ProxyServer server;

    public ServerCommand(ProxyServer server) {
        this.server = server;
    }

    @Override
    public void execute(CommandSource source, String[] args) {
        if (!(source instanceof Player)) {
            source.sendMessage(TextComponent.of("Only players may run this command.", TextColor.RED));
            return;
        }

        Player player = (Player) source;
        if (args.length == 1) {
            // Trying to connect to a server.
            String serverName = args[0];
            Optional<ServerInfo> toConnect = server.getServerInfo(serverName);
            if (!toConnect.isPresent()) {
                player.sendMessage(TextComponent.of("Server " + serverName + " doesn't exist.", TextColor.RED));
                return;
            }

            player.createConnectionRequest(toConnect.get()).fireAndForget();
        } else {
            String currentServer = player.getCurrentServer().map(ServerConnection::getServerInfo).map(ServerInfo::getName)
                    .orElse("<unknown>");
            player.sendMessage(TextComponent.of("You are currently connected to " + currentServer + ".", TextColor.YELLOW));

            // Assemble the list of servers as components
            TextComponent.Builder serverListBuilder = TextComponent.builder("Available servers: ").color(TextColor.YELLOW);
            List<ServerInfo> infos = ImmutableList.copyOf(server.getAllServers());
            for (int i = 0; i < infos.size(); i++) {
                ServerInfo serverInfo = infos.get(i);
                TextComponent infoComponent = TextComponent.of(serverInfo.getName());
                if (serverInfo.getName().equals(currentServer)) {
                    infoComponent = infoComponent.color(TextColor.GREEN)
                            .hoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, TextComponent.of("Currently connected to this server")));
                } else {
                    infoComponent = infoComponent.color(TextColor.GRAY)
                            .clickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/server " + serverInfo.getName()))
                            .hoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, TextComponent.of("Click to connect to this server")));
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
    public List<String> suggest(CommandSource source, String[] currentArgs) {
        if (currentArgs.length == 0) {
            return server.getAllServers().stream()
                    .map(ServerInfo::getName)
                    .collect(Collectors.toList());
        } else if (currentArgs.length == 1) {
            return server.getAllServers().stream()
                    .map(ServerInfo::getName)
                    .filter(name -> name.regionMatches(true, 0, currentArgs[0], 0, currentArgs[0].length()))
                    .collect(Collectors.toList());
        } else {
            return ImmutableList.of();
        }
    }
}
