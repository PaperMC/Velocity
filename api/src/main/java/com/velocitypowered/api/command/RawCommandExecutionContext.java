package com.velocitypowered.api.command;

/**
 * An execution context for a {@link RawCommand}.
 */
public interface RawCommandExecutionContext extends CommandExecutionContext {

  /**
   * Returns the full command line after the command name.
   *
   * @return the command line
   */
  String line();
}
