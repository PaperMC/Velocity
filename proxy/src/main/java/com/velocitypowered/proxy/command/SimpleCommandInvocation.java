package com.velocitypowered.proxy.command;

import com.google.common.base.Preconditions;
import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.context.CommandContext;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;

final class SimpleCommandInvocation extends AbstractCommandInvocation<String[]>
        implements SimpleCommand.Invocation {

  static final Factory FACTORY = new Factory();

  static class Factory implements CommandInvocationFactory<SimpleCommand.Invocation> {

    @Override
    public SimpleCommand.Invocation create(final CommandContext<CommandSource> context) {
      final String alias = CommandNodeFactory.readAlias(context);
      final String[] args = CommandNodeFactory.readArguments(
              context, String[].class, StringArrayArgumentType.EMPTY);
      return new SimpleCommandInvocation(context.getSource(), alias, args);
    }

    @Override
    public SimpleCommand.Invocation create(final ParseResults<CommandSource> parse) {
      final String alias = CommandNodeFactory.readAlias(parse);
      final String[] args = CommandNodeFactory.readArguments(
              parse, String[].class, StringArrayArgumentType.EMPTY);
      return new SimpleCommandInvocation(parse.getContext().getSource(), alias, args);
    }
  }

  private final String alias;

  private SimpleCommandInvocation(final CommandSource source, final String alias,
                                  final String[] arguments) {
    super(source, arguments);
    this.alias = Preconditions.checkNotNull(alias, "alias");
  }

  @Override
  public String alias() {
    return this.alias;
  }

  @Override
  public String toString() {
    return "SimpleCommandInvocation{"
            + "alias='" + alias + '\''
            + '}';
  }
}
