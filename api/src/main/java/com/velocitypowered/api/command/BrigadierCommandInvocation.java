package com.velocitypowered.api.command;

import com.google.errorprone.annotations.Immutable;
import com.mojang.brigadier.ParseResults;

/**
 * Contains the invocation data for a {@link BrigadierCommand}.
 */
@Immutable
public interface BrigadierCommandInvocation extends CommandInvocation {

  /**
   * Returns the valid parse results.
   *
   * @return the parsing results
   * @implNote the implementation may return an invalid parse result before
   *           the object is exposed
   */
  ParseResults<CommandSource> parsed();
}
