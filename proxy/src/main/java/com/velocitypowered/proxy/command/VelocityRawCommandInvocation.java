package com.velocitypowered.proxy.command;

import com.mojang.brigadier.context.CommandContext;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.RawCommand;

final class VelocityRawCommandInvocation extends AbstractCommandInvocation<String>
        implements RawCommand.Invocation {

  static final Factory FACTORY = new Factory();

  static class Factory implements CommandInvocationFactory<RawCommand.Invocation> {

    @Override
    public RawCommand.Invocation create(final CommandContext<CommandSource> context) {
      return new VelocityRawCommandInvocation(context.getSource(), getArguments(context));
    }
  }

  VelocityRawCommandInvocation(final CommandSource source, final String arguments) {
    super(source, arguments);
  }
}
