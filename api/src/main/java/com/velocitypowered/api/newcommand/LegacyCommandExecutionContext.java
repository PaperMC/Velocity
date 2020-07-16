package com.velocitypowered.api.newcommand;

import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * A {@link CommandExecutionContext} for {@link LegacyCommand}s.
 */
public interface LegacyCommandExecutionContext extends CommandExecutionContext {

    /**
     * Returns the arguments for the command execution.
     *
     * @return the arguments for the command
     */
    String @NonNull [] arguments();
}
