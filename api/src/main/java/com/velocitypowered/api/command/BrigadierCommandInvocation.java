package com.velocitypowered.api.command;

import com.mojang.brigadier.ParseResults;

/**
 * Contains the invocation data for a {@link BrigadierCommand}.
 */
public interface BrigadierCommandInvocation extends CommandInvocation {

  /**
   * Returns the result of parsing the given command.
   *
   * @return the parsing results
   */
  ParseResults<CommandSource> parsed();
}
