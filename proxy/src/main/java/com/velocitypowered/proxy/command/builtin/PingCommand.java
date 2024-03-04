package com.velocitypowered.proxy.command.builtin;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.permission.Tristate;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.Optional;

public class PingCommand {
    private final ProxyServer server;

    public PingCommand(ProxyServer server) {
        this.server = server;
    }

    public void register() {
        BrigadierCommand.literalArgumentBuilder("ping")
                .requires(source -> source.getPermissionValue("velocity.command.ping") != Tristate.FALSE)
                .then(
                        BrigadierCommand.requiredArgumentBuilder("player", StringArgumentType.word())
                                .executes(context -> {
                                    String player = context.getArgument("player", String.class);
                                    Optional<Player> maybePlayer = server.getPlayer(player);

                                    if (maybePlayer.isEmpty()) {
                                        context.getSource().sendMessage(
                                                CommandMessages.PLAYER_NOT_FOUND.arguments(Component.text(player))
                                        );
                                        return 0;
                                    }

                                    return this.getPing(context, maybePlayer.get());
                                })
                )
                .executes(context -> {
                    if (context.getSource() instanceof Player player) {
                        return this.getPing(context, player);
                    } else {
                        context.getSource().sendMessage(CommandMessages.PLAYERS_ONLY);
                        return 0;
                    }
                });
    }

    private int getPing(CommandContext<CommandSource> context, Player player) {
        long ping = player.getPing();

        if (ping == -1L) {
            context.getSource().sendMessage(
                    Component.translatable("velocity.command.ping.unknown", NamedTextColor.RED)
                            .arguments(Component.text(player.getUsername()))
            );

            return 0;
        }

        // Check if sending player matches for the response message.
        boolean matchesSender = false;

        if (context.getSource() instanceof Player sendingPlayer) {
            if (player.getUniqueId().equals(sendingPlayer.getUniqueId())) {
                matchesSender = true;
            }
        }

        if (matchesSender) {
            context.getSource().sendMessage(
                    Component.translatable("velocity.command.ping.other", NamedTextColor.GREEN)
                            .arguments(Component.text(ping))
            );
        } else {
            context.getSource().sendMessage(
                    Component.translatable("velocity.command.ping.yours", NamedTextColor.GREEN)
                            .arguments(Component.text(player.getUsername()), Component.text(ping))
            );
        }

        return Command.SINGLE_SUCCESS;
    }
}
