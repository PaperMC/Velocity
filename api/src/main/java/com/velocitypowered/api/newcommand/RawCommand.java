package com.velocitypowered.api.newcommand;

/**
 * A {@link Command} that accepts the raw arguments as a {@link String}.
 * This is useful when depending on external command frameworks.
 */
public interface RawCommand extends Command<RawCommandExecutionContext> {

    // TODO Suggestions, create SuggestableCommand interface?

    @Override
    default Type getType() {
        return Type.RAW;
    }
}
