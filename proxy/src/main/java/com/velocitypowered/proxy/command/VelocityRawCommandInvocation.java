package com.velocitypowered.proxy.command;

import com.google.common.base.Preconditions;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.RawCommandInvocation;

final class VelocityRawCommandInvocation extends AbstractCommandInvocation
        implements RawCommandInvocation {

  static final CommandInvocationFactory<RawCommandInvocation> FACTORY = new Factory();

  private static class Factory implements CommandInvocationFactory<RawCommandInvocation> {

    @Override
    public RawCommandInvocation create(final CommandSource source, final String commandLine) {
      return new VelocityRawCommandInvocation(source, commandLine);
    }
  }

  private final String commandLine;

  private VelocityRawCommandInvocation(final CommandSource source, final String commandLine) {
    super(source);
    this.commandLine = Preconditions.checkNotNull(commandLine);
  }

  @Override
  public String line() {
    return commandLine;
  }
}
