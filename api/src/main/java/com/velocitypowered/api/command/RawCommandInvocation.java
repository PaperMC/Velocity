package com.velocitypowered.api.command;

/**
 * Contains invocation data for a {@link RawCommand}.
 */
public interface RawCommandInvocation extends CommandInvocation {

  /**
   * Returns the full command line after the command name.
   *
   * @return the command line
   */
  String line();
}
