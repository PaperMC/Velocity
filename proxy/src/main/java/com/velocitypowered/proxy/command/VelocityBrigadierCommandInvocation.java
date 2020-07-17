package com.velocitypowered.proxy.command;

import com.google.common.base.Preconditions;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.ParseResults;
import com.velocitypowered.api.command.BrigadierCommandInvocation;
import com.velocitypowered.api.command.CommandSource;

final class VelocityBrigadierCommandInvocation extends AbstractCommandInvocation
        implements BrigadierCommandInvocation {

  static class Factory implements CommandInvocationFactory<BrigadierCommandInvocation> {

    private final CommandDispatcher<CommandSource> dispatcher;

    Factory(final CommandDispatcher<CommandSource> dispatcher) {
      this.dispatcher = Preconditions.checkNotNull(dispatcher, "dispatcher");
    }

    @Override
    public BrigadierCommandInvocation create(final CommandSource source, final String commandLine) {
      // Might be unsuccessful, checked on execution
      ParseResults<CommandSource> parse = dispatcher.parse(commandLine, source);
      return new VelocityBrigadierCommandInvocation(source, parse);
    }

    @Override
    public boolean includeAlias() {
      return true;
    }
  }

  private final ParseResults<CommandSource> parseResults;

  private VelocityBrigadierCommandInvocation(final CommandSource source,
                                             final ParseResults<CommandSource> parseResults) {
    super(source);
    this.parseResults = parseResults;
  }

  @Override
  public ParseResults<CommandSource> parsed() {
    return parseResults;
  }
}
