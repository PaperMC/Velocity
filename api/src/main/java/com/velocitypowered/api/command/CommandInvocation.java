package com.velocitypowered.api.command;

/**
 * Provides information related to the possible execution of a {@link Command}.
 */
public interface CommandInvocation {

  /**
   * Returns the source to execute the command for.
   *
   * @return the command source
   */
  CommandSource source();
}
