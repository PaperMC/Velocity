package com.velocitypowered.proxy.command;

import com.google.common.base.Preconditions;
import com.velocitypowered.api.command.CommandExecutionContext;
import com.velocitypowered.api.command.CommandSource;

abstract class AbstractCommandExecutionContext implements CommandExecutionContext {

  private final CommandSource source;

  protected AbstractCommandExecutionContext(final CommandSource source) {
    this.source = Preconditions.checkNotNull(source);
  }

  @Override
  public CommandSource source() {
    return source;
  }
}
