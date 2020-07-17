package com.velocitypowered.api.command;

import com.mojang.brigadier.ParseResults;

/**
 * Contains the invocation data for a {@link BrigadierCommand}.
 */
public interface BrigadierCommandInvocation extends CommandInvocation {

  /**
   * Returns the valid parse results.
   *
   * @return the parsing results
   * @implNote the implementation may return an invalid parse result before
   *           the context is exposed
   */
  ParseResults<CommandSource> parsed();
}
