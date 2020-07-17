package com.velocitypowered.api.command;

import com.mojang.brigadier.ParseResults;

/**
 * An execution context for a {@link BrigadierCommand}.
 */
public interface BrigadierCommandExecutionContext extends CommandExecutionContext {

  /**
   * Returns the valid parse results.
   *
   * @return the parsing results
   * @implNote the implementation may return an invalid parse result before the context
   *           is exposed via e.g. {@link Command#execute(CommandExecutionContext)}.
   */
  ParseResults<CommandSource> parsed();
}
