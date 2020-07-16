package com.velocitypowered.api.newcommand;

/**
 * A {@link CommandExecutionContext} for {@link LegacyCommand}s.
 */
public interface LegacyCommandExecutionContext extends CommandExecutionContext {

    /**
     * Returns the arguments for the command execution.
     *
     * @return the arguments for the command
     */
    String[] arguments();
}
