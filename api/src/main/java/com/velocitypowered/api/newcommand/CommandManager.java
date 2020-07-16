package com.velocitypowered.api.newcommand;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.event.command.CommandExecuteEvent;
import java.util.concurrent.CompletableFuture;

/**
 * Registers and executes commands.
 */
public interface CommandManager {

    /**
     * Registers the specified command.
     *
     * @param command the command to register
     */
    void register(Command<?> command);

    /**
     * Unregisters the specified command.
     *
     * @param command the command to unregister
     */
    void unregister(Command<?> command);

    /**
     * Unregisters the command with the specified alias.
     *
     * @param alias the alias of the command
     */
    // TODO Previous javadoc is ambiguous, does this unregister all the aliases of the command,
    // or just the given alias?
    void unregister(String alias);

    /**
     * Attempts to asynchronously execute a command from the given {@code cmdLine}.
     *
     * @param source the source to execute the command for
     * @param cmdLine the command to run
     * @return a future that may be completed with the result of the command execution.
     *         Can be completed exceptionally if an exception is thrown during execution.
     */
    CompletableFuture<Boolean> execute(CommandSource source, String cmdLine);

    /**
     * Attempts to asynchronously execute a command from the given {@code cmdLine}
     * without firing a {@link CommandExecuteEvent}.
     *
     * @param source the source to execute the command for
     * @param cmdLine the command to run
     * @return a future that may be completed with the result of the command execution.
     *         Can be completed exceptionally if an exception is thrown during execution.
     */
    CompletableFuture<Boolean> executeImmediately(CommandSource source, String cmdLine);
}
