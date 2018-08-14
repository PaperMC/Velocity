package com.velocitypowered.api.command;

/**
 * Represents an interface to register a command executor with the proxy.
 */
public interface CommandManager {
    void registerCommand(String name, Command command);

    void unregisterCommand(String name);

    boolean execute(CommandSource source, String cmdLine);
}
