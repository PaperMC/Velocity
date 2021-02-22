package com.velocitypowered.proxy.command;

import com.google.common.base.Preconditions;
import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.context.CommandContext;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.RawCommand;

final class RawCommandInvocation extends AbstractCommandInvocation<String>
        implements RawCommand.Invocation {

  static final Factory FACTORY = new Factory();

  static class Factory implements CommandInvocationFactory<RawCommand.Invocation> {

    @Override
    public RawCommand.Invocation create(final CommandContext<CommandSource> context) {
      final String alias = CommandNodeFactory.readAlias(context);
      final String args = CommandNodeFactory.readArguments(context, String.class, "");
      return new RawCommandInvocation(context.getSource(), alias, args);
    }

    @Override
    public RawCommand.Invocation create(final ParseResults<CommandSource> parse) {
      final String alias = CommandNodeFactory.readAlias(parse);
      final String args = CommandNodeFactory.readArguments(parse, String.class, "");
      return new RawCommandInvocation(parse.getContext().getSource(), alias, args);
    }
  }

  private final String alias;

  private RawCommandInvocation(final CommandSource source,
                               final String alias, final String arguments) {
    super(source, arguments);
    this.alias = Preconditions.checkNotNull(alias, "alias");
  }

  @Override
  public String alias() {
    return alias;
  }

  @Override
  public String toString() {
    return "RawCommandInvocation{"
            + "alias='" + alias + '\''
            + '}';
  }
}
