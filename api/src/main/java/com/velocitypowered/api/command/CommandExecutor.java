package com.velocitypowered.api.command;

import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.List;

/**
 * Represents an executor intended to be used by a {@link Command}.
 */
public interface CommandExecutor {
    /**
     * Executes the command for the specified {@link CommandSource}.
     * @param source the source of this command
     * @param args the arguments for this command
     */
    void execute(@NonNull CommandSource source, @NonNull String[] args);

    /**
     * Provides tab complete suggestions for a command for a specified {@link CommandSource}.
     * @param source the source to run the command for
     * @param args the current, partial arguments for this command
     * @return tab complete suggestions
     */
    List<String> suggest(@NonNull CommandSource source, @NonNull String[] args);
}
