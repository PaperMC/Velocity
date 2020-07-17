package com.velocitypowered.api.command;

/**
 * Provides information related to the possible execution of a {@link Command}.
 */
public interface CommandInvocation {

  /**
   * Returns the source that executed the command.
   *
   * @return the command source
   */
  CommandSource source();
}
