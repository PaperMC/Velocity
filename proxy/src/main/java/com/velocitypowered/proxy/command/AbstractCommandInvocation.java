package com.velocitypowered.proxy.command;

import com.google.common.base.Preconditions;
import com.google.errorprone.annotations.Immutable;
import com.velocitypowered.api.command.CommandInvocation;
import com.velocitypowered.api.command.CommandSource;

/**
 * Abstract base class for {@link CommandInvocation} implementations.
 */
@Immutable
abstract class AbstractCommandInvocation implements CommandInvocation {

  private final CommandSource source;

  protected AbstractCommandInvocation(final CommandSource source) {
    this.source = Preconditions.checkNotNull(source);
  }

  @Override
  public CommandSource source() {
    return source;
  }
}
