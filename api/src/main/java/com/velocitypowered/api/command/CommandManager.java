package com.velocitypowered.api.command;

import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * Represents an interface to register a command executor with the proxy.
 */
public interface CommandManager {
    void register(@NonNull Command command, String... aliases);

    void unregister(@NonNull String alias);

    boolean execute(@NonNull CommandSource source, @NonNull String cmdLine);
}
