package com.velocitypowered.api.newcommand;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.velocitypowered.api.command.CommandSource;

/**
 * A command that uses the Brigadier library for parsing.
 */
public interface BrigadierCommand extends Command<BrigadierCommandExecutionContext> {

    /**
     * A suggestion provider that returns no suggestions.
     */
    SuggestionProvider<CommandSource> NO_SUGGESTIONS = (context, builder) -> Suggestions.empty();

    /**
     * Returns the command tree node representing the syntax options for
     * the command.
     *
     * @return the command node
     */
    LiteralCommandNode<CommandSource> getRoot();

    /**
     * Returns the suggestion provider for the command.
     *
     * @return the suggestion provider
     */
    default SuggestionProvider<CommandSource> getSuggestionProvider() {
        return NO_SUGGESTIONS;
    }

    @Override
    default void execute(BrigadierCommandExecutionContext context) {
        try {
            context.dispatcher().execute(context.parsed());
        } catch (final CommandSyntaxException e) {
            // Implementation checks parse was successful
            throw new AssertionError(e);
        }
    }

    @Override
    default Type getType() {
        return Type.BRIGADIER;
    }
}
