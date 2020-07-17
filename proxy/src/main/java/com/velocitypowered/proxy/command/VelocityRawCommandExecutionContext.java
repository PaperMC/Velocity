package com.velocitypowered.proxy.command;

import com.google.common.base.Preconditions;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.RawCommandExecutionContext;

final class VelocityRawCommandExecutionContext extends AbstractCommandExecutionContext
        implements RawCommandExecutionContext {

  static final CommandExecutionContextFactory<RawCommandExecutionContext> FACTORY = new Factory();

  private static class Factory implements CommandExecutionContextFactory<RawCommandExecutionContext> {

    @Override
    public RawCommandExecutionContext createContext(final CommandSource source, final String commandLine) {
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
