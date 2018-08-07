package com.velocitypowered.api.command;

import com.google.common.collect.ImmutableList;

import javax.annotation.Nonnull;
import java.util.List;

/**
 * Represents a command that can be executed by a {@link CommandInvoker}, such as a {@link com.velocitypowered.api.proxy.Player}
 * or the console.
 */
public interface CommandExecutor {
    /**
     * Executes the command for the specified {@link CommandInvoker}.
     * @param invoker the invoker of this command
     * @param args the arguments for this command
     */
    void execute(@Nonnull CommandInvoker invoker, @Nonnull String[] args);

    /**
     * Provides tab complete suggestions for a command for a specified {@link CommandInvoker}.
     * @param invoker the invoker to run the command for
     * @param currentArgs the current, partial arguments for this command
     * @return tab complete suggestions
     */
    default List<String> suggest(@Nonnull CommandInvoker invoker, @Nonnull String[] currentArgs) {
        return ImmutableList.of();
    }
}
