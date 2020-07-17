package com.velocitypowered.proxy.command;

import com.google.common.base.Preconditions;
import com.velocitypowered.api.command.CommandInvocation;
import com.velocitypowered.api.command.CommandSource;

abstract class AbstractCommandExecutionContext implements CommandInvocation {

  private final CommandSource source;

  protected AbstractCommandExecutionContext(final CommandSource source) {
    this.source = Preconditions.checkNotNull(source);
  }

  @Override
  public CommandSource source() {
    return source;
  }
}
