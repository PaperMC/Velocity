package com.velocitypowered.proxy.command;

import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.velocitypowered.api.command.Command;
import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.command.CommandSource;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.*;

public class VelocityCommandManager implements CommandManager {
    private final Map<String, Command> commands = new HashMap<>();

    @Override
    public void register(@NonNull final Command command, final String... aliases) {
        Preconditions.checkNotNull(aliases, "aliases");
        Preconditions.checkNotNull(command, "executor");
        for (int i = 0, length = aliases.length; i < length; i++) {
            final String alias = aliases[i];
            Preconditions.checkNotNull(aliases, "alias at index %s", i);
            this.commands.put(alias.toLowerCase(Locale.ENGLISH), command);
        }
    }

    @Override
    public void unregister(@NonNull final String alias) {
        Preconditions.checkNotNull(alias, "name");
        this.commands.remove(alias.toLowerCase(Locale.ENGLISH));
    }

    @Override
    public boolean execute(@NonNull CommandSource source, @NonNull String cmdLine) {
        Preconditions.checkNotNull(source, "invoker");
        Preconditions.checkNotNull(cmdLine, "cmdLine");

        List<String> split = Splitter.on(" ").trimResults().omitEmptyStrings().splitToList(cmdLine);
        if (split.isEmpty()) {
            return false;
        }

        String alias = split.get(0);
        String[] actualArgs = split.subList(1, split.size()).toArray(new String[0]);
        Command command = commands.get(alias.toLowerCase(Locale.ENGLISH));
        if (command == null) {
            return false;
        }

        try {
            if (!command.hasPermission(source, actualArgs)) {
                return false;
            }

            command.execute(source, actualArgs);
            return true;
        } catch (Exception e) {
            throw new RuntimeException("Unable to invoke command " + cmdLine + " for " + source, e);
        }
    }

    public boolean hasCommand(String command) {
        return commands.containsKey(command);
    }

    public List<String> offerSuggestions(CommandSource source, String cmdLine) {
        Preconditions.checkNotNull(source, "source");
        Preconditions.checkNotNull(cmdLine, "cmdLine");

        List<String> split = Splitter.on(" ").trimResults().omitEmptyStrings().splitToList(cmdLine);
        if (split.isEmpty()) {
            return ImmutableList.of();
        }

        String alias = split.get(0);
        if (split.size() == 1) {
            List<String> availableCommands = new ArrayList<>();
            for (Map.Entry<String, Command> entry : commands.entrySet()) {
                if (entry.getKey().regionMatches(true, 0, alias, 0, alias.length()) &&
                        entry.getValue().hasPermission(source, new String[0])) {
                    availableCommands.add("/" + entry.getKey());
                }
            }
            return availableCommands;
        }

        String[] actualArgs = split.subList(1, split.size()).toArray(new String[0]);
        Command command = commands.get(alias.toLowerCase(Locale.ENGLISH));
        if (command == null) {
            return ImmutableList.of();
        }

        try {
            if (!command.hasPermission(source, actualArgs)) {
                return ImmutableList.of();
            }

            return command.suggest(source, actualArgs);
        } catch (Exception e) {
            throw new RuntimeException("Unable to invoke suggestions for command " + alias + " for " + source, e);
        }
    }
}
