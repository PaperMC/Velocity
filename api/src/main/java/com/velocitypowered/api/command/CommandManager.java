package com.velocitypowered.api.command;

/**
 * Represents an interface to register a command executor with the proxy.
 */
public interface CommandManager {
    void register(Command command, String... aliases);

    void unregister(String alias);

    boolean execute(CommandSource source, String cmdLine);
}
