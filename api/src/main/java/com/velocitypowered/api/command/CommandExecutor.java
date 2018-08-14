package com.velocitypowered.api.command;

import com.google.common.collect.ImmutableList;

import javax.annotation.Nonnull;
import java.util.List;

/**
 * Represents a command that can be executed by a {@link CommandSource}, such as a {@link com.velocitypowered.api.proxy.Player}
 * or the console.
 */
public interface CommandExecutor {
    /**
     * Executes the command for the specified {@link CommandSource}.
     * @param source the source of this command
     * @param args the arguments for this command
     */
    void execute(@Nonnull CommandSource source, @Nonnull String[] args);

    /**
     * Provides tab complete suggestions for a command for a specified {@link CommandSource}.
     * @param source the source to run the command for
     * @param currentArgs the current, partial arguments for this command
     * @return tab complete suggestions
     */
    default List<String> suggest(@Nonnull CommandSource source, @Nonnull String[] currentArgs) {
        return ImmutableList.of();
    }
}
