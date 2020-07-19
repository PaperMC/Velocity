package com.velocitypowered.proxy.command;

import com.mojang.brigadier.context.CommandContext;
import com.velocitypowered.api.command.CommandInvocation;
import com.velocitypowered.api.command.CommandSource;

/**
 * Creates command invocation contexts for the given {@link CommandSource}
 * and command line arguments.
 *
 * @param <I> the type of the built invocation
 */
@FunctionalInterface
public interface CommandInvocationFactory<I extends CommandInvocation<?>> {

  /**
   * Returns an invocation context for the given Brigadier context.
   *
   * @param context the command context
   * @return the built invocation context
   */
  I create(final CommandContext<CommandSource> context);

  /**
   * Returns the arguments after the command alias.
   *
   * @param context the command context
   * @return the parsed arguments, or an empty string if no arguments were given
   */
  default String getArguments(final CommandContext<CommandSource> context) {
    if (!context.getInput().contains(" ")) {
      return "";
    }

    return context.getArgument(VelocityCommandManager.ARGUMENTS_NAME, String.class);
  }
}
