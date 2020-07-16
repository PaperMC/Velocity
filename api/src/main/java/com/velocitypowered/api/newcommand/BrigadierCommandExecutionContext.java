package com.velocitypowered.api.newcommand;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.ParseResults;
import com.velocitypowered.api.command.CommandSource;

/**
 * A {@link CommandExecutionContext} for {@link BrigadierCommand}s.
 */
public interface BrigadierCommandExecutionContext extends CommandExecutionContext {

    /**
     * Returns the dispatcher in charge of executing the command.
     *
     * @return the command dispatcher
     */
    CommandDispatcher<CommandSource> dispatcher();

    // TODO This may be helpful, but a Brigadier command should strictly
    // only use the parse results.
    //String raw();

    /**
     * Returns the valid, parsed command.
     *
     * @return the parsed command
     */
    ParseResults<CommandSource> parsed();
}
