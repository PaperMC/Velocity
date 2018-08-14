package com.velocitypowered.proxy.command;

import com.google.common.collect.ImmutableList;
import com.velocitypowered.api.command.CommandExecutor;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.server.ServerInfo;
import com.velocitypowered.proxy.VelocityServer;
import net.kyori.text.TextComponent;
import net.kyori.text.format.TextColor;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class ServerCommand implements CommandExecutor {
    @Override
    public void execute(@Nonnull CommandSource invoker, @Nonnull String[] args) {
        if (!(invoker instanceof Player)) {
            invoker.sendMessage(TextComponent.of("Only players may run this command.", TextColor.RED));
            return;
        }

        Player player = (Player) invoker;
        if (args.length == 1) {
            // Trying to connect to a server.
            String serverName = args[0];
            Optional<ServerInfo> server = VelocityServer.getServer().getServerInfo(serverName);
            if (!server.isPresent()) {
                player.sendMessage(TextComponent.of("Server " + serverName + " doesn't exist.", TextColor.RED));
                return;
            }

            player.createConnectionRequest(server.get()).fireAndForget();
        } else {
            String serverList = VelocityServer.getServer().getAllServers().stream()
                    .map(ServerInfo::getName)
                    .collect(Collectors.joining(", "));
            player.sendMessage(TextComponent.of("Available servers: " + serverList, TextColor.YELLOW));
        }
    }

    @Override
    public List<String> suggest(@Nonnull CommandSource invoker, @Nonnull String[] currentArgs) {
        if (currentArgs.length == 0) {
            return VelocityServer.getServer().getAllServers().stream()
                    .map(ServerInfo::getName)
                    .collect(Collectors.toList());
        } else if (currentArgs.length == 1) {
            return VelocityServer.getServer().getAllServers().stream()
                    .map(ServerInfo::getName)
                    .filter(name -> name.regionMatches(true, 0, currentArgs[0], 0, currentArgs[0].length()))
                    .collect(Collectors.toList());
        } else {
            return ImmutableList.of();
        }
    }
}
