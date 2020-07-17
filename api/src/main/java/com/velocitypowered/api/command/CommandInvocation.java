package com.velocitypowered.api.command;

/**
 * Provides {@link Command} invocation-related information, including
 * the source, arguments, etc.
 */
public interface CommandInvocation {

  /**
   * Returns the source that executed the command.
   *
   * @return the command source
   */
  CommandSource source();
}
