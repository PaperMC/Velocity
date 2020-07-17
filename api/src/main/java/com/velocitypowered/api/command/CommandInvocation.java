package com.velocitypowered.api.command;

import com.google.errorprone.annotations.Immutable;

/**
 * Provides information related to the possible execution of a {@link Command}.
 */
@Immutable
public interface CommandInvocation {

  /**
   * Returns the source that executed the command.
   *
   * @return the command source
   */
  CommandSource source();
}
