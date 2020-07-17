package com.velocitypowered.api.command;

import com.google.errorprone.annotations.Immutable;

/**
 * Contains the invocation data for a {@link RawCommand}.
 */
@Immutable
public interface RawCommandInvocation extends CommandInvocation {

  /**
   * Returns the full command line after the command alias.
   *
   * @return the command line
   */
  String line();
}
