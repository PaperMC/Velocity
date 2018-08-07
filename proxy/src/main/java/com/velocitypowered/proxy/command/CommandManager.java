package com.velocitypowered.proxy.command;

import com.google.common.base.Preconditions;
import com.velocitypowered.api.command.CommandExecutor;
import com.velocitypowered.api.command.CommandInvoker;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class CommandManager {
    private final Map<String, CommandExecutor> executors = new HashMap<>();

    public void registerCommand(String name, CommandExecutor executor) {
        Preconditions.checkNotNull(name, "name");
        Preconditions.checkNotNull(executor, "executor");
        this.executors.put(name, executor);
    }

    public void unregisterCommand(String name) {
        Preconditions.checkNotNull(name, "name");
        this.executors.remove(name);
    }

    public boolean execute(CommandInvoker invoker, String cmdLine) {
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
}
