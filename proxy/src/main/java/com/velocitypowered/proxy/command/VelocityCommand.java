package com.velocitypowered.proxy.command;

import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.velocitypowered.api.command.Command;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.permission.Tristate;
import com.velocitypowered.proxy.VelocityServer;
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
    private final Map<String, Command> subcommands = ImmutableMap.<String, Command>builder()
            .put("version", Info.INSTANCE)
            .build();

    private void usage(CommandSource source) {
        String commandText = "/velocity <" + String.join("|", subcommands.keySet()) + ">";
        source.sendMessage(TextComponent.of(commandText, TextColor.RED));
    }

    @Override
    public void execute(CommandSource source, String[] args) {
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
    public List<String> suggest(@NonNull CommandSource source, @NonNull String[] currentArgs) {
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
    public boolean hasPermission(@NonNull CommandSource source, @NonNull String[] args) {
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
        static final Info INSTANCE = new Info();
        private Info() {}

        @Override
        public void execute(@NonNull CommandSource source, @NonNull String[] args) {
            String implName = MoreObjects.firstNonNull(VelocityServer.class.getPackage().getImplementationTitle(), "Velocity");
            String implVersion = MoreObjects.firstNonNull(VelocityServer.class.getPackage().getImplementationVersion(), "<unknown>");
            String implVendor = MoreObjects.firstNonNull(VelocityServer.class.getPackage().getImplementationVendor(), "Velocity Contributors");
            TextComponent velocity = TextComponent.builder(implName + " ")
                    .decoration(TextDecoration.BOLD, true)
                    .color(TextColor.DARK_AQUA)
                    .append(TextComponent.of(implVersion).decoration(TextDecoration.BOLD, false))
                    .build();
            TextComponent copyright = TextComponent.of("Copyright 2018 " + implVendor + ". " + implName + " is freely licensed under the terms of the " +
                    "MIT License.");
            source.sendMessage(velocity);
            source.sendMessage(copyright);

            if (implName.equals("Velocity")) {
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
        public boolean hasPermission(@NonNull CommandSource source, @NonNull String[] args) {
            return source.getPermissionValue("velocity.command.info") != Tristate.FALSE;
        }
    }
}
