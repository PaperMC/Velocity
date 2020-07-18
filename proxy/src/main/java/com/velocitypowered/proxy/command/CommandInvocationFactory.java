package com.velocitypowered.proxy.command;

import com.velocitypowered.api.command.CommandInvocation;
import com.velocitypowered.api.command.CommandSource;

/**
 * Creates command invocation contexts for the given {@link CommandSource} and
 * command line.
 *
 * @param <I> the type of the command invocation
 */
@FunctionalInterface
public interface CommandInvocationFactory<I extends CommandInvocation> {

  CommandInvocationFactory<VelocityLegacyCommandInvocation> FALLBACK =
          new VelocityLegacyCommandInvocation.Factory(true);

  /**
   * Returns an invocation context for the given source and command line.
   *
   * @param source the source that executed the command
   * @param commandLine the command line, including the alias if
   *        {@link #includeAlias()} is {@code true}
   * @return the built invocation
   */
  I create(final CommandSource source, final String commandLine);

  /**
   * Returns whether the factory expects the command line to include the command alias.
   *
   * @return {@code true} if the factory expects the command alias
   */
  default boolean includeAlias() {
    return false;
  }
}
