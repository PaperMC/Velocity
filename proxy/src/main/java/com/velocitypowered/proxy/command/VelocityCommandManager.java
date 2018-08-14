package com.velocitypowered.proxy.command;

import com.google.common.base.Preconditions;
import com.velocitypowered.api.command.CommandExecutor;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.CommandManager;

import java.util.*;
import java.util.stream.Collectors;

public class VelocityCommandManager implements CommandManager {
    private final Map<String, CommandExecutor> executors = new HashMap<>();

    @Override
    public void registerCommand(String name, CommandExecutor executor) {
        Preconditions.checkNotNull(name, "name");
        Preconditions.checkNotNull(executor, "executor");
        this.executors.put(name, executor);
    }

    @Override
    public void unregisterCommand(String name) {
        Preconditions.checkNotNull(name, "name");
        this.executors.remove(name);
    }

    @Override
    public boolean execute(CommandSource invoker, String cmdLine) {
        Preconditions.checkNotNull(invoker, "invoker");
        Preconditions.checkNotNull(cmdLine, "cmdLine");

        String[] split = cmdLine.split(" ", -1);
        if (split.length == 0) {
            return false;
        }

        String command = split[0];
        String[] actualArgs = Arrays.copyOfRange(split, 1, split.length);
        CommandExecutor executor = executors.get(command);
        if (executor == null) {
            return false;
        }

        try {
            executor.execute(invoker, actualArgs);
            return true;
        } catch (Exception e) {
            throw new RuntimeException("Unable to invoke command " + cmdLine + " for " + invoker, e);
        }
    }

    public Optional<List<String>> offerSuggestions(CommandSource invoker, String cmdLine) {
        Preconditions.checkNotNull(invoker, "invoker");
        Preconditions.checkNotNull(cmdLine, "cmdLine");

        String[] split = cmdLine.split(" ", -1);
        if (split.length == 0) {
            return Optional.empty();
        }

        String command = split[0];
        if (split.length == 1) {
            return Optional.of(executors.keySet().stream()
                    .filter(cmd -> cmd.regionMatches(true, 0, command, 0, command.length()))
                    .collect(Collectors.toList()));
        }

        String[] actualArgs = Arrays.copyOfRange(split, 1, split.length);
        CommandExecutor executor = executors.get(command);
        if (executor == null) {
            return Optional.empty();
        }

        try {
            return Optional.of(executor.suggest(invoker, actualArgs));
        } catch (Exception e) {
            throw new RuntimeException("Unable to invoke suggestions for command " + command + " for " + invoker, e);
        }
    }
}
