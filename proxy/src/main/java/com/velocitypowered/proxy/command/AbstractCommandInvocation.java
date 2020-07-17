package com.velocitypowered.proxy.command;

import com.google.common.base.Preconditions;
import com.velocitypowered.api.command.CommandInvocation;
import com.velocitypowered.api.command.CommandSource;

/**
 * Abstract base class for {@link CommandInvocation} implementations.
 */
abstract class AbstractCommandInvocation implements CommandInvocation {

  private final CommandSource source;

  protected AbstractCommandInvocation(final CommandSource source) {
    this.source = Preconditions.checkNotNull(source, "source");
  }

  @Override
  public CommandSource source() {
    return source;
  }
}
