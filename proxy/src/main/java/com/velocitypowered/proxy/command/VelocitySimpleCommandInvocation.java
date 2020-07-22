package com.velocitypowered.proxy.command;

import com.mojang.brigadier.context.CommandContext;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.proxy.util.BrigadierUtils;

final class VelocitySimpleCommandInvocation extends AbstractCommandInvocation<String[]>
        implements SimpleCommand.Invocation {

  static final Factory FACTORY = new Factory();

  static class Factory implements CommandInvocationFactory<SimpleCommand.Invocation> {

    @Override
    public SimpleCommand.Invocation create(final CommandContext<CommandSource> context) {
      final String[] arguments = BrigadierUtils.getSplitArguments(context);
      return new VelocitySimpleCommandInvocation(context.getSource(), arguments);
    }
  }

  VelocitySimpleCommandInvocation(final CommandSource source, final String[] arguments) {
    super(source, arguments);
  }
}
