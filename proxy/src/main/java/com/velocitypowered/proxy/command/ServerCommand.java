package com.velocitypowered.proxy.command;

import com.velocitypowered.api.command.CommandExecutor;
import com.velocitypowered.api.command.CommandInvoker;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.server.ServerInfo;
import com.velocitypowered.proxy.VelocityServer;
import net.kyori.text.TextComponent;
import net.kyori.text.format.TextColor;

import javax.annotation.Nonnull;
import java.util.Optional;
import java.util.stream.Collectors;

public class ServerCommand implements CommandExecutor {
    @Override
    public void execute(@Nonnull CommandInvoker invoker, @Nonnull String[] args) {
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
}
