package com.velocitypowered.proxy.command;

import com.google.common.base.Preconditions;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.RawCommandInvocation;

final class VelocityRawCommandExecutionContext extends AbstractCommandExecutionContext
        implements RawCommandInvocation {

  static final CommandInvocationFactory<RawCommandInvocation> FACTORY = new Factory();

  private static class Factory implements CommandInvocationFactory<RawCommandInvocation> {

    @Override
    public RawCommandInvocation createContext(final CommandSource source, final String commandLine) {
      return new VelocityRawCommandExecutionContext(source, commandLine);
    }
  }

  private final String commandLine;

  private VelocityRawCommandExecutionContext(final CommandSource source, final String commandLine) {
    super(source);
    this.commandLine = Preconditions.checkNotNull(commandLine);
  }

  @Override
  public String line() {
    return commandLine;
  }
}
