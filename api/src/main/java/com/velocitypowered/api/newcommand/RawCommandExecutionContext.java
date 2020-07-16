package com.velocitypowered.api.newcommand;

/**
 * A {@link CommandExecutionContext} for {@link RawCommand}s.
 */
public interface RawCommandExecutionContext extends CommandExecutionContext {

    /**
     * Returns the arguments for the command execution.
     *
     * @return the raw arguments
     */
    String arguments();
}
