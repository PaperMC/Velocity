package com.velocitypowered.proxy.command;

import com.google.common.base.Preconditions;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.ParseResults;
import com.velocitypowered.api.command.BrigadierCommandInvocation;
import com.velocitypowered.api.command.CommandSource;

final class VelocityBrigadierCommandExecutionContext extends AbstractCommandExecutionContext
        implements BrigadierCommandInvocation {

  static class Factory implements CommandExecutionContextFactory<BrigadierCommandInvocation> {

    private final CommandDispatcher<CommandSource> dispatcher;

    Factory(final CommandDispatcher<CommandSource> dispatcher) {
      this.dispatcher = Preconditions.checkNotNull(dispatcher);
    }

    @Override
    public BrigadierCommandInvocation createContext(final CommandSource source, final String commandLine) {
      // Might be unsuccessful, checked on execution
      ParseResults<CommandSource> parse = dispatcher.parse(commandLine, source);
      return new VelocityBrigadierCommandExecutionContext(source, parse);
    }

    @Override
    public boolean argsCommandLine() {
      return false; // full command line
    }
  }

  private final ParseResults<CommandSource> parseResults;

  private VelocityBrigadierCommandExecutionContext(final CommandSource source,
                                                   final ParseResults<CommandSource> parseResults) {
    super(source);
    this.parseResults = parseResults;
  }

  @Override
  public ParseResults<CommandSource> parsed() {
    return parseResults;
  }
}
