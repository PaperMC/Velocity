package com.velocitypowered.api.command;

import com.mojang.brigadier.ParseResults;

/**
 * An execution context for a {@link BrigadierCommand}.
 */
public interface BrigadierCommandExecutionContext extends CommandExecutionContext {

  // TODO this may be helpful, but Brigadier commands should strictly used the parsed results.
  // String raw();

  ParseResults<CommandSource> parsed();
}
