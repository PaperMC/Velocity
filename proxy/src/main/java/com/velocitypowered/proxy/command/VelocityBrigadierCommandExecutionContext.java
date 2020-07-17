package com.velocitypowered.proxy.command;

import com.google.common.base.Preconditions;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.velocitypowered.api.command.BrigadierCommandExecutionContext;
import com.velocitypowered.api.command.CommandSource;

final class VelocityBrigadierCommandExecutionContext extends AbstractCommandExecutionContext
        implements BrigadierCommandExecutionContext {

  static class Factory implements CommandExecutionContextFactory<BrigadierCommandExecutionContext> {

    private final CommandDispatcher<CommandSource> dispatcher;

    Factory(final CommandDispatcher<CommandSource> dispatcher) {
      this.dispatcher = Preconditions.checkNotNull(dispatcher);
    }

    @Override
    public BrigadierCommandExecutionContext createContext(final CommandSource source, final String commandLine)
            throws CommandSyntaxException {
      ParseResults<CommandSource> parse = dispatcher.parse(commandLine, source);

      if (parse.getReader().canRead()) {
        // Parsing was unsuccessful, retrieve exception. This is similar to
        // https://github.com/Mojang/brigadier/blob/8e9859e4712f06ad287f8c0a18725309151778ec/src/main/java/com/mojang/brigadier/CommandDispatcher.java#L208,
        // but the command is always found.
        if (parse.getExceptions().size() == 1) {
          throw parse.getExceptions().values().iterator().next();
        } else {
          throw CommandSyntaxException.BUILT_IN_EXCEPTIONS
                  .dispatcherUnknownArgument()
                  .createWithContext(parse.getReader());
        }
      }

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
