package com.velocitypowered.api.newcommand;

import com.google.common.collect.ImmutableList;
import java.util.List;

/**
 * A legacy 1.12-style command.
 */
public interface LegacyCommand extends Command<LegacyCommandExecutionContext> {

    /**
     * Provides tab complete suggestions for the given execution context.
     *
     * @param context the execution context
     * @return the tab complete suggestions
     */
    default List<String> suggest(LegacyCommandExecutionContext context) {
        return ImmutableList.of();
    }

    @Override
    default Type getType() {
        return Type.LEGACY;
    }
}
