package com.velocitypowered.proxy.command;

import com.google.common.base.Preconditions;
import com.velocitypowered.api.command.CommandInvocation;
import com.velocitypowered.api.command.CommandSource;

/**
 * Abstract base class for {@link CommandInvocation} implementations.
 *
 * @param <T> the type of the arguments
 */
abstract class AbstractCommandInvocation<T> implements CommandInvocation<T> {

  private final CommandSource source;
  private final T arguments;

  protected AbstractCommandInvocation(final CommandSource source, final T arguments) {
    this.source = Preconditions.checkNotNull(source, "source");
    this.arguments = Preconditions.checkNotNull(arguments, "arguments");
  }

  @Override
  public CommandSource source() {
    return source;
  }

  @Override
  public T arguments() {
    return arguments;
  }
}
