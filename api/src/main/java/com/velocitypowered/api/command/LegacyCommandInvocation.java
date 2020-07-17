package com.velocitypowered.api.command;

import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * Contains the invocation data for a {@link LegacyCommand}.
 */
public interface LegacyCommandInvocation extends CommandInvocation {

  /**
   * Returns the arguments for the command invocation.
   *
   * @return the command arguments
   */
  String @NonNull [] arguments();
}
