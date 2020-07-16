package com.velocitypowered.api.command;

import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * An execution context for a {@link LegacyCommand}.
 */
public interface LegacyCommandExecutionContext extends CommandExecutionContext {

  /**
   * Returns the arguments for the command execution.
   *
   * @return the command arguments
   */
  String @NonNull [] arguments();
}
