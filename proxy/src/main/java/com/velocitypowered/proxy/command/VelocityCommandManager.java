package com.velocitypowered.proxy.command;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.velocitypowered.api.command.Command;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.CommandManager;

import java.util.*;
import java.util.stream.Collectors;

public class VelocityCommandManager implements CommandManager {
    private final Map<String, Command> commands = new HashMap<>();

    @Override
    public void register(final Command command, final String... aliases) {
        Preconditions.checkNotNull(aliases, "aliases");
        Preconditions.checkNotNull(command, "executor");
        for (int i = 0, length = aliases.length; i < length; i++) {
            final String alias = aliases[i];
            Preconditions.checkNotNull(aliases, "alias at index %s", i);
            this.commands.put(alias.toLowerCase(Locale.ENGLISH), command);
        }
    }

    @Override
    public void unregister(final String alias) {
        Preconditions.checkNotNull(alias, "name");
        this.commands.remove(alias.toLowerCase(Locale.ENGLISH));
    }

    @Override
    public boolean execute(CommandSource source, String cmdLine) {
        Preconditions.checkNotNull(source, "invoker");
        Preconditions.checkNotNull(cmdLine, "cmdLine");

        String[] split = cmdLine.split(" ", -1);
        if (split.length == 0) {
            return false;
        }

        String alias = split[0];
        String[] actualArgs = Arrays.copyOfRange(split, 1, split.length);
        Command command = commands.get(alias.toLowerCase(Locale.ENGLISH));
        if (command == null) {
            return false;
        }

        try {
            command.execute(source, actualArgs);
            return true;
        } catch (Exception e) {
            throw new RuntimeException("Unable to invoke command " + cmdLine + " for " + source, e);
        }
    }

    public Optional<List<String>> offerSuggestions(CommandSource source, String cmdLine) {
        Preconditions.checkNotNull(source, "source");
        Preconditions.checkNotNull(cmdLine, "cmdLine");

        String[] split = cmdLine.split(" ", -1);
        if (split.length == 0) {
            return Optional.empty();
        }

        String command = split[0];
        if (split.length == 1) {
            return Optional.of(commands.keySet().stream()
                    .filter(cmd -> cmd.regionMatches(true, 0, command, 0, command.length()))
                    .collect(Collectors.toList()));
        }

        String[] actualArgs = Arrays.copyOfRange(split, 1, split.length);
        Command executor = commands.get(command);
        if (executor == null) {
            return Optional.empty();
        }

        try {
            return Optional.of(executor.suggest(source, actualArgs));
        } catch (Exception e) {
            throw new RuntimeException("Unable to invoke suggestions for command " + command + " for " + source, e);
        }
    }
}
