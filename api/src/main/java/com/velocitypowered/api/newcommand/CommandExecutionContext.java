package com.velocitypowered.api.newcommand;

import com.velocitypowered.api.command.CommandSource;

/**
 * Provides data about the invocation of a {@link Command},
 * including the {@link CommandSource} caller, command arguments, etc.
 */
public interface CommandExecutionContext {

    /**
     * Returns the source that executed the command.
     *
     * @return the command source
     */
    CommandSource source();
}
