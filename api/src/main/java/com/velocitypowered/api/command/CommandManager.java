package com.velocitypowered.api.command;

/**
 * Represents an interface to register a command executor with the proxy.
 */
public interface CommandManager {
    void registerCommand(String name, CommandExecutor executor);

    void unregisterCommand(String name);

    boolean execute(CommandSource invoker, String cmdLine);
}
