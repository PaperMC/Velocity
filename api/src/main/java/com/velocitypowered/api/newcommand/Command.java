package com.velocitypowered.api.newcommand;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;

/**
 * Represents a command that can be executed by a {@link CommandSource},
 * such as a {@link Player} or the console.
 *
 * @param <C> the type of the execution context
 */
public interface Command<C extends CommandExecutionContext> {

    /**
     * Represents the command type, which determines
     * its parsing capabilities.
     */
    // TODO Introduce CommandMeta and move there?
    enum Type {
        /**
         * Uses the Brigadier library to parse commands.
         *
         * @see BrigadierCommand
         */
        BRIGADIER,
        /**
         * 1.12-style command parsing.
         *
         * @see LegacyCommand
         */
        LEGACY,
        /**
         * Accepts raw arguments as a {@link String}
         *
         * @see RawCommand
         */
        RAW
    }

    /**
     * Returns the type of the command.
     *
     * @return the command type
     */
    Type getType();

    /**
     * Executes the command for the specified context.
     *
     * @param context the execution context
     */
    void execute(C context);

    /**
     * Test to check if the source has permission to use this command with
     * the provided arguments.
     *
     * <p>If the method returns {@code false}, the handling is forwarded onto
     * the players current server.
     *
     * @param context the execution context
     * @return {@code true} if the source has permission
     */
    default boolean hasPermission(C context) {
        return true;
    }
}
