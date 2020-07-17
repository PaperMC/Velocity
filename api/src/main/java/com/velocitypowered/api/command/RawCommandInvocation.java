package com.velocitypowered.api.command;

/**
 * Contains the invocation data for a {@link RawCommand}.
 */
public interface RawCommandInvocation extends CommandInvocation {

  /**
   * Returns the full command line after the command alias.
   *
   * @return the command line
   */
  String line();
}
