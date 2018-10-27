package com.velocitypowered.proxy.command;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.velocitypowered.api.command.Command;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.permission.Tristate;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.util.ProxyVersion;
import net.kyori.text.TextComponent;
import net.kyori.text.event.ClickEvent;
import net.kyori.text.format.TextColor;
import net.kyori.text.format.TextDecoration;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

public class VelocityCommand implements Command {
    private final Map<String, Command> subcommands;

    public VelocityCommand(ProxyServer server) {
        this.subcommands = ImmutableMap.<String, Command>builder()
                .put("version", new Info(server))
                .build();
    }

    private void usage(@NonNull CommandSource source) {
        String commandText = "/velocity <" + String.join("|", subcommands.keySet()) + ">";
        source.sendMessage(TextComponent.of(commandText, TextColor.RED));
    }

    @Override
    public void execute(@NonNull CommandSource source, @NonNull String @NonNull [] args) {
        if (args.length == 0) {
            usage(source);
            return;
        }

        Command command = subcommands.get(args[0].toLowerCase(Locale.US));
        if (command == null) {
            usage(source);
            return;
        }
        command.execute(source, Arrays.copyOfRange(args, 1, args.length));
    }

    @Override
    public List<String> suggest(@NonNull CommandSource source, String @NonNull [] currentArgs) {
        if (currentArgs.length == 0) {
            return ImmutableList.copyOf(subcommands.keySet());
        }

        if (currentArgs.length == 1) {
            return subcommands.keySet().stream()
                    .filter(name -> name.regionMatches(true, 0, currentArgs[0], 0, currentArgs[0].length()))
                    .collect(Collectors.toList());
        }

        Command command = subcommands.get(currentArgs[0].toLowerCase(Locale.US));
        if (command == null) {
            return ImmutableList.of();
        }
        return command.suggest(source, Arrays.copyOfRange(currentArgs, 1, currentArgs.length));
    }

    @Override
    public boolean hasPermission(@NonNull CommandSource source, @NonNull String @NonNull [] args) {
        if (args.length == 0) {
            return true;
        }
        Command command = subcommands.get(args[0].toLowerCase(Locale.US));
        if (command == null) {
            return true;
        }
        return command.hasPermission(source, Arrays.copyOfRange(args, 1, args.length));
    }

    private static class Info implements Command {
        private final ProxyServer server;

        private Info(ProxyServer server) {
            this.server = server;
        }

        @Override
        public void execute(@NonNull CommandSource source, @NonNull String @NonNull [] args) {
            ProxyVersion version = server.getVersion();

            TextComponent velocity = TextComponent.builder(version.getName() + " ")
                    .decoration(TextDecoration.BOLD, true)
                    .color(TextColor.DARK_AQUA)
                    .append(TextComponent.of(version.getVersion()).decoration(TextDecoration.BOLD, false))
                    .build();
            TextComponent copyright = TextComponent.of("Copyright 2018 " + version.getVendor() + ". " + version.getName() + " is freely licensed under the terms of the " +
                    "MIT License.");
            source.sendMessage(velocity);
            source.sendMessage(copyright);

            if (version.getName().equals("Velocity")) {
                TextComponent velocityWebsite = TextComponent.builder()
                        .content("Visit the ")
                        .append(TextComponent.builder("Velocity website")
                                .color(TextColor.GREEN)
                                .clickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, "https://www.velocitypowered.com"))
                                .build())
                        .append(TextComponent.of(" or the ").resetStyle())
                        .append(TextComponent.builder("Velocity GitHub")
                                .color(TextColor.GREEN)
                                .clickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, "https://github.com/VelocityPowered/Velocity"))
                                .build())
                        .build();
                source.sendMessage(velocityWebsite);
            }
        }

        @Override
        public boolean hasPermission(@NonNull CommandSource source, @NonNull String @NonNull [] args) {
            return source.getPermissionValue("velocity.command.info") != Tristate.FALSE;
        }
    }
}
