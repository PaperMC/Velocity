package com.velocitypowered.api.command;

import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.List;
import java.util.Optional;

/**
 * Represents an interface to register and build commands with the proxy.
 */
public interface CommandManager {

    /**
     * Registers the specified command.
     * @param command the command to register
     */
    void register(@NonNull Command command);

    /**
     * Unregisters a command with the specified alias.
     * @param alias the alias of which the command should be unregistered
     */
    void unregister(@NonNull String alias);

    /**
     * Specifies whether this CommandManager has a command by the given alias.
     * @param alias the alias to query
     * @return true if the command is registered
     */
    boolean hasCommand(@NonNull String alias);

    /**
     * Gets an {@link Optional} {@link List} of tab completions.
     * @param source the source of the input
     * @param rawInput the input to suggest a completion for
     * @return the Optional List of tab completions
     */
    Optional<List<String>> offerSuggestions(@NonNull CommandSource source, @NonNull String rawInput);

    /**
     * Attempts to execute a command from the specified {@code rawInput}.
     * @param source the command's source
     * @param rawInput the command to run
     * @return true if the command was found and executed
     */
    boolean execute(@NonNull CommandSource source, @NonNull String rawInput);

    /**
     * Gets a builder so that commands can be built and registered.
     * @param alias the trigger for the potential command
     * @param aliases the alternative triggers for the potential Command
     * @return the builder
     */
    CommandBuilder builder(@NonNull String alias, String... aliases);
}
