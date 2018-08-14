package com.velocitypowered.api.command;

import com.google.common.collect.ImmutableList;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.List;

/**
 * Represents a command that can be executed by a {@link CommandSource}, such as a {@link com.velocitypowered.api.proxy.Player}
 * or the console.
 */
public interface Command {
    /**
     * Executes the command for the specified {@link CommandSource}.
     * @param source the source of this command
     * @param args the arguments for this command
     */
    void execute(@NonNull CommandSource source, @NonNull String[] args);

    /**
     * Provides tab complete suggestions for a command for a specified {@link CommandSource}.
     * @param source the source to run the command for
     * @param currentArgs the current, partial arguments for this command
     * @return tab complete suggestions
     */
    default List<String> suggest(@NonNull CommandSource source, @NonNull String[] currentArgs) {
        return ImmutableList.of();
    }
}
